package com.folio.notes.ui

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.folio.notes.R
import com.folio.notes.data.BackupCodec
import com.folio.notes.data.Feed
import com.folio.notes.data.FolioSettings
import com.folio.notes.data.Note
import com.folio.notes.data.NotesRepository
import com.folio.notes.data.SortMode
import com.folio.notes.logic.FolioSchemes
import com.folio.notes.logic.KeepImport
import com.folio.notes.logic.asFolioNote
import com.folio.notes.ui.components.FolioDialog
import com.folio.notes.ui.components.FolioDialogActions
import com.folio.notes.ui.components.FolioDialogBody
import com.folio.notes.ui.components.FolioDialogTitle
import com.folio.notes.ui.components.NoteAction
import com.folio.notes.ui.components.LocalOverlayBlur
import com.folio.notes.ui.components.NoteActionSheet
import com.folio.notes.ui.components.overlayBackdropBlur
import com.folio.notes.ui.components.rememberOverlayBlurState
import com.folio.notes.ui.components.expandFrom
import com.folio.notes.ui.screens.EditorScreen
import com.folio.notes.ui.screens.HomeScreen
import com.folio.notes.ui.screens.SettingsScreen
import com.folio.notes.ui.screens.TagsScreen
import com.folio.notes.ui.theme.LocalFolioMotion
import com.folio.notes.ui.theme.LocalFolioSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** The four surfaces the app can show. Editor carries the note id, not the note. */
sealed interface Screen {
    data object Home : Screen

    /**
     * [origin] is the window rect of the tile this note was opened from, when there
     * was one. It rides on the screen itself so the transition spec can tell a note
     * being unfolded out of its own card apart from any other arrival.
     */
    data class Editor(
        val id: String,
        val openTint: Boolean = false,
        val origin: Rect? = null,
    ) : Screen

    data object Tags : Screen
    data object Settings : Screen
}

/** How far into the stack a surface sits; a deeper screen arrives from the right. */
internal fun screenDepth(screen: Screen): Int = when (screen) {
    Screen.Home -> 0
    Screen.Tags -> 1
    Screen.Settings -> 1
    is Screen.Editor -> 2
}

/** True while the stack is being pushed rather than popped. */
internal fun screenMovesForward(from: Screen, to: Screen): Boolean =
    screenDepth(to) >= screenDepth(from)

/**
 * True when [to] is an editor that should grow out of the tile it was opened from.
 * A note opened from the action sheet, or a fresh note, has no tile and pans instead.
 */
internal fun opensFromTile(to: Screen): Boolean = to is Screen.Editor && to.origin != null

private sealed interface Pending {
    data class ConfirmDelete(val note: Note) : Pending
    data class ConfirmEmptyTrash(val count: Int) : Pending
}

@Composable
fun FolioRoot(
    notes: List<Note>,
    settings: FolioSettings,
    repository: NotesRepository,
    versionName: String,
    onSettings: ((FolioSettings) -> FolioSettings) -> Unit,
    modifier: Modifier = Modifier,
    sharedText: String? = null,
    onSharedTextConsumed: () -> Unit = {},
) {
    val context = LocalContext.current
    val motion = LocalFolioMotion.current
    val scheme = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val overlayBlur = rememberOverlayBlurState()

    var screen by remember { mutableStateOf<Screen>(Screen.Home) }
    var feed by remember { mutableStateOf(Feed.ALL) }
    var query by remember { mutableStateOf("") }
    var tagFilter by remember { mutableStateOf<String?>(null) }
    var sort by remember { mutableStateOf(settings.sortMode) }
    var sheetNote by remember { mutableStateOf<Note?>(null) }
    var pending by remember { mutableStateOf<Pending?>(null) }

    /** Blank notes are drafts, not documents: leaving one throws it away. */
    fun persist(note: Note) {
        // A draft can outlive the state it was built on: the note may have been archived,
        // trashed or deleted while the editor was still on screen. Never resurrect a
        // deleted note, and let the store own the flags the editor never edits.
        val stored = repository.note(note.id) ?: return
        val merged = note.copy(archived = stored.archived, trashed = stored.trashed)
        val blank = merged.title.isBlank() && merged.body.isBlank() && merged.tags.isEmpty()
        if (blank) {
            repository.deleteForever(merged.id)
        } else {
            repository.save(merged)
        }
    }

    fun goHome() {
        query = ""
        screen = Screen.Home
    }

    fun toast(message: String) {
        scope.launch { snackbar.showSnackbar(message) }
    }

    fun importKeep(uri: Uri) {
        toast(context.getString(R.string.toast_keep_importing))
        scope.launch {
            val parsed = withContext(Dispatchers.IO) {
                runCatching {
                    val name = readDisplayName(context, uri)
                    val mime = context.contentResolver.getType(uri).orEmpty()
                    val isZip = name.endsWith(".zip", ignoreCase = true) ||
                        mime == "application/zip" ||
                        mime == "application/x-zip-compressed"
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        if (isZip) {
                            // Keep archives can contain large attachments; parse the ZIP
                            // entries directly and never load those attachment bytes.
                            KeepImport.parseZip(stream)
                        } else {
                            val bytes = KeepImport.readLimited(stream) ?: error("too large")
                            KeepImport.parse(name, bytes)
                        }
                    } ?: error("empty import")
                }.getOrNull()
            }
            if (parsed == null || parsed.notes.isEmpty()) {
                toast(
                    context.getString(
                        if (parsed == null) R.string.toast_keep_import_failed else R.string.toast_keep_import_empty,
                    ),
                )
                return@launch
            }
            val before = repository.notes.value.size
            repository.appendAll(parsed.notes.map { it.asFolioNote() })
            val added = repository.notes.value.size - before
            if (added <= 0) {
                toast(context.getString(R.string.toast_keep_none_new))
            } else {
                toast(context.resources.getQuantityString(R.plurals.toast_keep_imported, added, added))
            }
        }
    }

    // The editor handles its own back press so a pending autosave is flushed first.
    BackHandler(enabled = screen != Screen.Home && screen !is Screen.Editor) {
        screen = Screen.Home
    }

    // Navigation is spatial, not decorative: a deeper screen travels in from the
    // right edge and the screen it covers drifts out at a quarter of the distance,
    // so the two surfaces read as one stack being pushed sideways. Nothing scales
    // and nothing overshoots - a page that springs past its own edge shows the void
    // behind it, and a full-screen scale forces the GPU to resample every pixel of
    // the aurora behind it for 300 ms, which is what made the old transition cost
    // twice the frames of a static screen.
    val panMillis = motion.millis(320)
    val panSpec = tween<IntOffset>(panMillis, easing = FastOutSlowInEasing)
    val expandMillis = motion.millis(380)
    // Closing runs slightly quicker than opening: leaving should feel like letting go.
    val collapseMillis = motion.millis(300)

    fun transformFor(from: Screen, to: Screen): ContentTransform {
        // Opened from a tile: the editor is that tile unfolding, so it has no page
        // travel of its own. Home recedes underneath while the editor grows over it.
        if (opensFromTile(to)) {
            return EnterTransition.None togetherWith fadeOut(tween(motion.millis(260)))
        }
        // Closing the same editor: the page folds back into the tile, and the list it
        // was covering is simply already there - the clip shrinking is the whole
        // effect. Fading the list in as well would show the window behind it through
        // the gap the clip is opening.
        if (from is Screen.Editor && from.origin != null && to !is Screen.Editor) {
            return (EnterTransition.None togetherWith
                fadeOut(tween(collapseMillis), targetAlpha = 1f)).apply {
                // Negative index puts the exiting editor above the returning list.
                targetContentZIndex = -1f
            }
        }
        return if (screenMovesForward(from, to)) {
            (slideInHorizontally(panSpec) { width -> width } +
                fadeIn(tween(motion.millis(220)))) togetherWith
                (slideOutHorizontally(panSpec) { width -> -width / 4 } +
                    fadeOut(tween(motion.millis(180))))
        } else {
            // Mirror of the push: the list comes back from a quarter width out and
            // the screen being closed travels the full width to the right.
            (slideInHorizontally(panSpec) { width -> -width / 4 } +
                fadeIn(tween(motion.millis(220)))) togetherWith
                (slideOutHorizontally(panSpec) { width -> width } +
                    fadeOut(tween(motion.millis(180))))
        }
    }

    val expandProgress = remember { mutableFloatStateOf(1f) }
    var expandOrigin by remember { mutableStateOf<Rect?>(null) }
    var windowSize by remember { mutableStateOf(IntSize.Zero) }
    // Window rects are read once, at the instant a tile is tapped, so a plain map
    // keeps them off the recomposition path while the list scrolls.
    val cardBounds = remember { HashMap<String, Rect>() }

    var previousScreen by remember { mutableStateOf<Screen?>(null) }
    LaunchedEffect(screen) {
        val from = previousScreen
        val to = screen
        previousScreen = to
        val opening = to is Screen.Editor && to.origin != null
        val closing = from is Screen.Editor && from.origin != null && !opening
        if (!opening && !closing) return@LaunchedEffect
        // Keep the origin around while the surface collapses, clear it only once the
        // page is gone so a later sheet-opened editor cannot inherit a stale tile.
        expandOrigin = (to as? Screen.Editor)?.origin ?: (from as Screen.Editor).origin
        if (motion.reduced) {
            expandProgress.floatValue = if (opening) 1f else 0f
            return@LaunchedEffect
        }
        if (opening) expandProgress.floatValue = 0f
        animate(
            initialValue = expandProgress.floatValue,
            targetValue = if (opening) 1f else 0f,
            animationSpec = tween(
                if (opening) expandMillis else collapseMillis,
                easing = FastOutSlowInEasing,
            ),
        ) { value, _ -> expandProgress.floatValue = value }
        expandProgress.floatValue = if (opening) 1f else 0f
    }

    /** Every route into the editor goes through here, so the origin never leaks. */
    fun openEditor(note: Note, origin: Rect? = null, openTint: Boolean = false) {
        expandOrigin = origin
        expandProgress.floatValue = if (origin == null || motion.reduced) 1f else 0f
        screen = Screen.Editor(note.id, openTint = openTint, origin = origin)
    }

    // Text shared from another app becomes a normal Folio note. Wait for the
    // initial database read so the new note cannot be overwritten by load().
    LaunchedEffect(sharedText) {
        val incoming = sharedText?.trim().orEmpty()
        if (incoming.isEmpty()) return@LaunchedEffect
        repository.loaded.first { it }
        val note = repository.create()
        repository.save(note.copy(body = incoming))
        feed = Feed.ALL
        query = ""
        tagFilter = null
        openEditor(note)
        onSharedTextConsumed()
    }

    CompositionLocalProvider(LocalOverlayBlur provides overlayBlur) {
    Box(
        modifier
            .fillMaxSize()
            .onSizeChanged { windowSize = it },
    ) {
        AnimatedContent(
            targetState = screen,
            transitionSpec = { transformFor(initialState, targetState) },
            label = "screens",
            modifier = Modifier
                .fillMaxSize()
                .overlayBackdropBlur(
                    active = overlayBlur.depth > 0 || sheetNote != null || pending != null,
                ),
        ) { current ->
            when (current) {
                Screen.Home -> HomeScreen(
                    notes = notes,
                    feed = feed,
                    onFeedChange = { feed = it },
                    query = query,
                    onQueryChange = { query = it },
                    tag = tagFilter,
                    onTagChange = { tagFilter = it },
                    sort = sort,
                    onSortChange = {
                        sort = it
                        onSettings { s -> s.copy(sortMode = it) }
                    },
                    onOpen = { note -> openEditor(note, cardBounds[note.id]) },
                    onNew = {
                        val note = repository.create()
                        openEditor(note)
                    },
                    onLongPress = { sheetNote = it },
                    onOpenSettings = { screen = Screen.Settings },
                    onOpenTags = { screen = Screen.Tags },
                    snackbarVisible = snackbar.currentSnackbarData != null,
                    onCardBounds = { note, rect -> cardBounds[note.id] = rect },
                )

                is Screen.Editor -> {
                    val note = notes.firstOrNull { it.id == current.id }
                    if (note == null) {
                        LaunchedEffect(current.id) { screen = Screen.Home }
                    } else {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .expandFrom(expandProgress, expandOrigin, windowSize),
                        ) {
                            EditorScreen(
                                note = note,
                                openTint = current.openTint,
                                onBack = { screen = Screen.Home },
                                onPersist = ::persist,
                                onArchive = {
                                    repository.archive(note.id)
                                    screen = Screen.Home
                                    toast(context.getString(R.string.toast_archived))
                                },
                                onTrash = {
                                    repository.trash(note.id)
                                    screen = Screen.Home
                                    toast(context.getString(R.string.toast_trashed))
                                },
                                onDuplicate = {
                                    val copy = note.copy(
                                        id = java.util.UUID.randomUUID().toString(),
                                        createdAt = System.currentTimeMillis(),
                                        updatedAt = System.currentTimeMillis(),
                                    )
                                    repository.save(copy)
                                    openEditor(copy)
                                    toast(context.getString(R.string.toast_duplicated))
                                },
                            )
                        }
                    }
                }

                Screen.Tags -> TagsScreen(
                    tags = com.folio.notes.logic.NoteQuery.allTags(notes),
                    onBack = { screen = Screen.Home },
                    onSelect = { tag ->
                        feed = Feed.ALL
                        query = ""
                        tagFilter = tag
                        screen = Screen.Home
                    },
                    onRename = { from, to -> repository.renameTag(from, to) },
                    onDelete = { repository.deleteTag(it) },
                )

                Screen.Settings -> SettingsScreen(
                    settings = settings,
                    onUpdate = onSettings,
                    noteCount = notes.count { !it.trashed },
                    trashedCount = notes.count { it.trashed },
                    versionName = versionName,
                    onBack = { screen = Screen.Home },
                    onExport = { uri -> exportTo(context, uri, notes, ::toast) },
                    onImport = { uri -> importFrom(context, uri, repository, ::toast) },
                    onImportKeep = ::importKeep,
                    onEmptyTrash = {
                        pending = Pending.ConfirmEmptyTrash(notes.count { it.trashed })
                    },
                    onSeedSamples = {
                        repository.appendAll(sampleNotes())
                        toast(context.getString(R.string.toast_samples_added))
                    },
                )
            }
        }

        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 24.dp, vertical = 22.dp),
        ) { data ->
            // Match the palette instead of Material's white-on-dark inverse surface, which
            // fights both the earthy light theme and AMOLED black.
            Snackbar(
                snackbarData = data,
                containerColor = scheme.surfaceContainerHigh,
                contentColor = scheme.onSurface,
                actionContentColor = scheme.primary,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.border(
                    width = 1.dp,
                    color = scheme.outlineVariant.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(20.dp),
                ),
            )
        }
    }

    val sheet = sheetNote
    if (sheet != null) {
        val live = notes.firstOrNull { it.id == sheet.id } ?: sheet
        NoteActionSheet(
            note = live,
            onDismiss = { sheetNote = null },
            onAction = { action ->
                sheetNote = null
                when (action) {
                    NoteAction.Pin -> repository.togglePin(live.id)
                    NoteAction.Archive -> repository.archive(live.id)
                    NoteAction.Unarchive -> repository.unarchive(live.id)
                    NoteAction.Trash -> {
                        repository.trash(live.id)
                        toast(context.getString(R.string.toast_trashed))
                    }
                    NoteAction.Restore -> repository.restore(live.id)
                    NoteAction.DeleteForever -> pending = Pending.ConfirmDelete(live)
                    NoteAction.Duplicate -> {
                        val copy = live.copy(
                            id = java.util.UUID.randomUUID().toString(),
                            createdAt = System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis(),
                        )
                        repository.save(copy)
                        toast(context.getString(R.string.toast_duplicated))
                    }
                    NoteAction.Tag -> openEditor(live)
                    NoteAction.Tint -> openEditor(live, openTint = true)
                }
            },
        )
    }

    when (val p = pending) {
        is Pending.ConfirmDelete -> FolioDialog(onDismissRequest = { pending = null }) {
            FolioDialogTitle(stringResource(R.string.dialog_delete_title))
            FolioDialogBody(stringResource(R.string.dialog_delete_message))
            FolioDialogActions(
                confirmLabel = stringResource(R.string.action_delete),
                onConfirm = {
                    repository.deleteForever(p.note.id)
                    pending = null
                    toast(context.getString(R.string.toast_deleted))
                },
                dismissLabel = stringResource(R.string.action_keep),
                onDismiss = { pending = null },
                destructive = true,
            )
        }

        is Pending.ConfirmEmptyTrash -> FolioDialog(onDismissRequest = { pending = null }) {
            FolioDialogTitle(stringResource(R.string.dialog_empty_title))
            FolioDialogBody(
                stringResource(
                    R.string.dialog_empty_message,
                    pluralStringResource(R.plurals.note_count, p.count, p.count),
                ),
            )
            FolioDialogActions(
                confirmLabel = stringResource(R.string.action_empty),
                onConfirm = {
                    repository.emptyTrash()
                    pending = null
                    toast(context.getString(R.string.toast_trash_emptied))
                },
                dismissLabel = stringResource(R.string.action_cancel),
                onDismiss = { pending = null },
                destructive = true,
            )
        }

        null -> Unit
    }
    }
}

private fun exportTo(
    context: Context,
    uri: Uri,
    notes: List<Note>,
    toast: (String) -> Unit,
) {
    runCatching {
        context.contentResolver.openOutputStream(uri)?.use { stream ->
            stream.write(BackupCodec.encode(notes).toByteArray(Charsets.UTF_8))
        }
    }.onSuccess {
        val count = context.resources.getQuantityString(R.plurals.note_count, notes.size, notes.size)
        toast(context.getString(R.string.toast_exported, count))
    }.onFailure {
        toast(context.getString(R.string.toast_export_failed))
    }
}

private fun readDisplayName(context: Context, uri: Uri): String {
    val projection = arrayOf(OpenableColumns.DISPLAY_NAME)
    val name = runCatching {
        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }.getOrNull()
    return name?.takeIf { it.isNotBlank() } ?: uri.lastPathSegment ?: "google-keep-import"
}

private fun importFrom(
    context: Context,
    uri: Uri,
    repository: NotesRepository,
    toast: (String) -> Unit,
) {
    runCatching {
        val text = context.contentResolver.openInputStream(uri)?.use { stream ->
            stream.readBytes().toString(Charsets.UTF_8)
        }.orEmpty()
        val incoming = BackupCodec.decode(text)
        repository.appendAll(incoming)
        incoming.size
    }.onSuccess { count ->
        val notes = context.resources.getQuantityString(R.plurals.note_count, count, count)
        toast(context.getString(R.string.toast_imported, notes))
    }.onFailure {
        toast(context.getString(R.string.toast_import_failed))
    }
}

/** A small tour, written to show off every block the renderer knows. */
private fun sampleNotes(): List<Note> {
    val now = System.currentTimeMillis()
    return listOf(
        Note(
            id = java.util.UUID.randomUUID().toString(),
            title = "Welcome to Folio",
            body = """
                # A quiet place for loud ideas

                Folio is a notebook first and an app second. Everything here is plain
                text, so nothing you write gets locked in.

                ## The small dialect

                - **Bold** and _italic_ and ~~struck~~ and `code`
                - [x] A task that is already done
                - [ ] A task still waiting
                  - and a nested one

                > Writing is thinking with a paper trail.

                ```kotlin
                fun greet(name: String) = "Hello, ${'$'}name"
                ```

                Tap the eye in the editor to see this rendered, then tap it back.
            """.trimIndent(),
            tint = 0,
            pinned = true,
            tags = listOf("intro", "folio"),
            createdAt = now - 90_000,
            updatedAt = now - 60_000,
        ),
        Note(
            id = java.util.UUID.randomUUID().toString(),
            title = "Weekend list",
            body = """
                - [ ] Espresso at the corner place
                - [ ] Finish the last chapter
                - [x] Water the fig tree
                - [ ] Call Mum

                ## If it rains

                Stay in and reread the good parts.
            """.trimIndent(),
            tint = 2,
            tags = listOf("personal"),
            createdAt = now - 3_600_000,
            updatedAt = now - 1_800_000,
        ),
        Note(
            id = java.util.UUID.randomUUID().toString(),
            title = "Colour notes",
            body = """
                Each note can carry its own paper colour. Long press any card and pick
                **Tint**, or open a note and use the palette menu.

                Tags are typed inline, and the tag library collects them across every
                note you have ever written.
            """.trimIndent(),
            tint = 4,
            tags = listOf("tips"),
            createdAt = now - 86_400_000,
            updatedAt = now - 43_200_000,
        ),
    )
}
