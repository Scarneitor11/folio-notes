package com.folio.notes.ui.screens

import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FormatBold
import androidx.compose.material.icons.automirrored.rounded.FormatIndentIncrease
import androidx.compose.material.icons.rounded.FormatItalic
import androidx.compose.material.icons.automirrored.rounded.FormatListBulleted
import androidx.compose.material.icons.rounded.FindReplace
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.folio.notes.R
import com.folio.notes.data.Note
import com.folio.notes.logic.EditorOps
import com.folio.notes.logic.FolioSchemes
import com.folio.notes.logic.MarkdownLite
import com.folio.notes.logic.NoteExport
import com.folio.notes.logic.NoteQuery
import com.folio.notes.logic.TextEdit
import com.folio.notes.ui.components.AuroraBackground
import com.folio.notes.ui.components.FolioDialog
import com.folio.notes.ui.components.FolioDialogActions
import com.folio.notes.ui.components.FolioDialogField
import com.folio.notes.ui.components.FolioDialogTitle
import com.folio.notes.ui.components.FolioMark
import com.folio.notes.ui.components.GlassPanel
import com.folio.notes.ui.components.MarkdownBody
import com.folio.notes.ui.components.MiniIconButton
import com.folio.notes.ui.components.Reveal
import com.folio.notes.ui.components.Rise
import com.folio.notes.ui.components.RollingContent
import com.folio.notes.ui.components.SectionLabel
import com.folio.notes.ui.components.TagChip
import com.folio.notes.ui.components.clickableNoRipple
import com.folio.notes.ui.components.entranceSheen
import com.folio.notes.ui.components.pan
import com.folio.notes.ui.components.pop
import com.folio.notes.ui.components.pressScale
import com.folio.notes.ui.components.pressTilt
import com.folio.notes.ui.components.rememberHaptics
import com.folio.notes.ui.components.rememberPopScale
import com.folio.notes.ui.components.Haptic
import com.folio.notes.ui.theme.Fraunces
import com.folio.notes.ui.theme.LocalAccent
import com.folio.notes.ui.theme.LocalFolioDark
import com.folio.notes.ui.theme.LocalFolioMotion
import com.folio.notes.ui.theme.LocalFolioSettings
import kotlinx.coroutines.delay

/**
 * Focus mode for a single note. The body is a plain text field over the
 * markdown-lite dialect with a toolbar that edits the live selection, and a
 * preview that renders the same document as real typography.
 */
@Composable
fun EditorScreen(
    note: Note,
    onBack: () -> Unit,
    onPersist: (Note) -> Unit,
    onArchive: () -> Unit,
    onTrash: () -> Unit,
    onDuplicate: () -> Unit,
    modifier: Modifier = Modifier,
    openTint: Boolean = false,
) {
    val settings = LocalFolioSettings.current
    val dark = LocalFolioDark.current
    val accent = LocalAccent.current
    val scheme = MaterialTheme.colorScheme
    val haptic = rememberHaptics()
    val motion = LocalFolioMotion.current
    val context = LocalContext.current

    var draft by remember(note.id) { mutableStateOf(note) }
    var title by remember(note.id) {
        mutableStateOf(TextFieldValue(note.title, TextRange(note.title.length)))
    }
    var body by remember(note.id) {
        mutableStateOf(TextFieldValue(note.body, TextRange(note.body.length)))
    }
    var preview by remember(note.id) { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var tagDialog by remember { mutableStateOf(false) }
    var tintOpen by remember(note.id) { mutableStateOf(openTint) }
    var tagInput by remember { mutableStateOf("") }
    var findOpen by remember { mutableStateOf(false) }
    var findQuery by remember { mutableStateOf("") }
    var replaceWith by remember { mutableStateOf("") }
    var matchCase by remember { mutableStateOf(false) }
    var findIndex by remember { mutableIntStateOf(-1) }
    var findJumped by remember { mutableStateOf(false) }
    var replaceHistory by remember(note.id) { mutableStateOf<List<ReplaceUndo>>(emptyList()) }
    var replaceRedo by remember(note.id) { mutableStateOf<List<ReplaceUndo>>(emptyList()) }
    var templatesOpen by remember { mutableStateOf(false) }
    val bodyFocus = remember { FocusRequester() }
    val undoSnackbar = remember { SnackbarHostState() }
    // Body stats are read by both the edit and preview toolbars; one scan per edit.
    val draftStats = remember(draft.body) { EditorOps.stats(draft.body) }

    val applyReplaceState: (ReplaceState) -> Unit = { state ->
        body = TextFieldValue(state.text, state.selection)
        draft = draft.copy(body = state.text)
        findIndex = state.findIndex
        preview = state.preview
        findJumped = true
        if (!findOpen) runCatching { bodyFocus.requestFocus() }
    }

    val restoreReplace: (ReplaceUndo) -> Unit = { edit ->
        if (replaceHistory.lastOrNull() === edit) {
            if (draft.body == edit.after.text) {
                applyReplaceState(edit.before)
                replaceHistory = replaceHistory.dropLast(1)
                replaceRedo = (replaceRedo + edit).takeLast(MAX_REPLACE_HISTORY)
                haptic(Haptic.Tap)
            } else {
                replaceHistory = emptyList()
                replaceRedo = emptyList()
            }
        }
    }

    val redoReplace: (ReplaceUndo) -> Unit = { edit ->
        if (replaceRedo.lastOrNull() === edit) {
            if (draft.body == edit.before.text) {
                applyReplaceState(edit.after)
                replaceRedo = replaceRedo.dropLast(1)
                replaceHistory = (replaceHistory + edit).takeLast(MAX_REPLACE_HISTORY)
                haptic(Haptic.Tap)
            } else {
                replaceHistory = emptyList()
                replaceRedo = emptyList()
            }
        }
    }

    // A manual edit makes both replace histories stale.
    LaunchedEffect(draft.body) {
        val undo = replaceHistory.lastOrNull()
        val redo = replaceRedo.lastOrNull()
        val undoValid = undo == null ||
            draft.body == undo.before.text || draft.body == undo.after.text
        val redoValid = redo == null ||
            draft.body == redo.before.text || draft.body == redo.after.text
        if (!undoValid || !redoValid) {
            replaceHistory = emptyList()
            replaceRedo = emptyList()
        }
    }

    // Snackbar Undo walks the undo stack; once exhausted, Redo restores a step.
    LaunchedEffect(replaceHistory, replaceRedo, findOpen) {
        if (findOpen) return@LaunchedEffect
        val undo = replaceHistory.lastOrNull()
        val redo = replaceRedo.lastOrNull()
        if (undo != null) {
            if (draft.body != undo.after.text) return@LaunchedEffect
            val result = undoSnackbar.showSnackbar(
                message = context.getString(R.string.editor_replace_undo_message),
                actionLabel = context.getString(R.string.editor_undo_replace),
                withDismissAction = true,
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) {
                restoreReplace(undo)
            } else if (replaceHistory.lastOrNull() === undo) {
                replaceHistory = emptyList()
                replaceRedo = emptyList()
            }
        } else if (redo != null) {
            if (draft.body != redo.before.text) return@LaunchedEffect
            val result = undoSnackbar.showSnackbar(
                message = context.getString(R.string.editor_replace_redo_message),
                actionLabel = context.getString(R.string.editor_redo_replace),
                withDismissAction = true,
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) {
                redoReplace(redo)
            } else if (replaceRedo.lastOrNull() === redo) {
                replaceHistory = emptyList()
                replaceRedo = emptyList()
            }
        }
    }

    // Quiet autosave: typing settles for a moment, then the draft is written.
    LaunchedEffect(draft.title, draft.body, draft.tags, draft.tint, draft.pinned) {
        if (draft == note) return@LaunchedEffect
        delay(650)
        onPersist(draft)
    }

    // When the find dialog closes after a jump, focus the body so the selected
    // match is visibly highlighted instead of only changing hidden state.
    LaunchedEffect(findOpen) {
        if (!findOpen && findJumped) {
            findJumped = false
            runCatching { bodyFocus.requestFocus() }
        }
    }

    // Back belongs to the editor rather than to FolioRoot, so a press inside the
    // autosave window still writes the draft instead of discarding it.
    BackHandler {
        onPersist(draft)
        onBack()
    }

    // Leaving for any other reason - archive, duplicate, rotation, process death -
    // flushes the pending debounce too. Only changed content is written, so opening
    // a note and closing it does not restamp its place in the feed.
    val liveDraft by rememberUpdatedState(draft)
    val liveNote by rememberUpdatedState(note)
    val livePersist by rememberUpdatedState(onPersist)
    DisposableEffect(note.id) {
        onDispose {
            if (!liveDraft.sameContentAs(liveNote)) livePersist(liveDraft)
        }
    }

    fun apply(edit: TextEdit) {
        val text = edit.text
        body = TextFieldValue(
            text = text,
            selection = TextRange(
                edit.start.coerceIn(0, text.length),
                edit.end.coerceIn(0, text.length),
            ),
        )
        draft = draft.copy(body = text)
        haptic(Haptic.Toggle)
    }

    fun updateBody(value: TextFieldValue) {
        // Pressing Enter on a list or quote line carries the marker to the next line;
        // Enter on an empty item leaves the list. Only a single typed newline triggers it,
        // so pasting a multi-line block is never rewritten.
        val typedNewline = value.selection.collapsed &&
            value.selection.start in 1..value.text.length &&
            value.text[value.selection.start - 1] == '\n' &&
            value.text.length == body.text.length + 1 &&
            value.text.removeRange(value.selection.start - 1, value.selection.start) == body.text
        if (typedNewline) {
            val continued = EditorOps.continueList(value.text, value.selection.start)
            if (continued.text != value.text) {
                body = TextFieldValue(continued.text, TextRange(continued.start, continued.end))
                draft = draft.copy(body = continued.text)
                return
            }
        }
        body = value
        if (draft.body != value.text) draft = draft.copy(body = value.text)
    }

    Box(modifier.fillMaxSize()) {
        AuroraBackground(Modifier.fillMaxSize(), accent = accent, blur = settings.blur)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .imePadding(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    // Frosted top scrim so the note can slide underneath cleanly.
                    .background(
                        Brush.verticalGradient(
                            0f to scheme.surface.copy(alpha = 0.94f),
                            0.7f to scheme.surface.copy(alpha = 0.86f),
                            1f to scheme.surface.copy(alpha = 0.0f),
                        ),
                    )
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MiniIconButton(
                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.action_back),
                    onClick = {
                        onPersist(draft)
                        onBack()
                    },
                    container = scheme.surface.copy(alpha = if (settings.blur) 0.55f else 0.9f),
                )
                Spacer(Modifier.width(12.dp))
                Rise(index = 0, modifier = Modifier.weight(1f)) {
                    Column {
                        Text(
                            text = stringResource(R.string.editor_editing),
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.4.sp),
                            fontWeight = FontWeight.Bold,
                            color = scheme.onSurfaceVariant.copy(alpha = 0.6f),
                        )
                        Text(
                            text = NoteQuery.relativeTime(
                                System.currentTimeMillis(),
                                draft.updatedAt,
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.onSurfaceVariant.copy(alpha = 0.45f),
                        )
                    }
                }
                MiniIconButton(
                    icon = Icons.Rounded.PushPin,
                    contentDescription = if (draft.pinned) {
                        stringResource(R.string.editor_unpin)
                    } else {
                        stringResource(R.string.editor_pin)
                    },
                    onClick = {
                        haptic(Haptic.Toggle)
                        draft = draft.copy(pinned = !draft.pinned)
                    },
                    tint = if (draft.pinned) accent.primary else scheme.onSurfaceVariant,
                    container = if (draft.pinned) {
                        accent.primary.copy(alpha = 0.18f)
                    } else {
                        scheme.surface.copy(alpha = if (settings.blur) 0.55f else 0.9f)
                    },
                )
                Spacer(Modifier.width(8.dp))
                MiniIconButton(
                    icon = if (preview) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                    contentDescription = if (preview) {
                        stringResource(R.string.editor_edit)
                    } else {
                        stringResource(R.string.editor_preview)
                    },
                    onClick = {
                        haptic(Haptic.Tap)
                        preview = !preview
                    },
                    tint = if (preview) accent.primary else scheme.onSurfaceVariant,
                    container = if (preview) {
                        accent.primary.copy(alpha = 0.18f)
                    } else {
                        scheme.surface.copy(alpha = if (settings.blur) 0.55f else 0.9f)
                    },
                )
                Spacer(Modifier.width(8.dp))
                Box {
                    MiniIconButton(
                        icon = Icons.Rounded.MoreVert,
                        contentDescription = stringResource(R.string.editor_more),
                        onClick = { menuOpen = true },
                        container = scheme.surface.copy(alpha = if (settings.blur) 0.55f else 0.9f),
                    )
                    DropdownMenu(
                        expanded = menuOpen,
                        onDismissRequest = { menuOpen = false },
                        shape = RoundedCornerShape(18.dp),
                        containerColor = scheme.surfaceContainerHigh,
                    ) {
                        EditorMenuItem(stringResource(R.string.sheet_add_tag), Icons.Rounded.Checklist) {
                            menuOpen = false
                            tagDialog = true
                        }
                        EditorMenuItem(stringResource(R.string.sheet_note_colour), Icons.Rounded.Palette) {
                            menuOpen = false
                            tintOpen = !tintOpen
                        }
                        EditorMenuItem(stringResource(R.string.editor_templates), Icons.Rounded.AutoAwesome) {
                            menuOpen = false
                            templatesOpen = true
                        }
                        EditorMenuItem(stringResource(R.string.editor_find_replace), Icons.Rounded.FindReplace) {
                            menuOpen = false
                            findIndex = -1
                            findJumped = false
                            findOpen = true
                        }
                        EditorMenuItem(stringResource(R.string.editor_share), Icons.Rounded.Share) {
                            menuOpen = false
                            shareNote(context, draft)
                        }
                        EditorMenuItem(stringResource(R.string.sheet_duplicate), Icons.Rounded.ContentCopy) {
                            menuOpen = false
                            onPersist(draft)
                            onDuplicate()
                        }
                        EditorMenuItem(stringResource(R.string.sheet_archive), Icons.Rounded.Archive) {
                            menuOpen = false
                            onPersist(draft)
                            onArchive()
                        }
                        EditorMenuItem(stringResource(R.string.sheet_move_to_trash), Icons.Rounded.Delete, danger = true) {
                            menuOpen = false
                            onPersist(draft)
                            onTrash()
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 22.dp, end = 22.dp, top = 18.dp, bottom = 132.dp),
            ) {
                BasicTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        draft = draft.copy(title = it.text)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.displaySmall.copy(
                        fontFamily = if (settings.serifTitles) Fraunces else MaterialTheme.typography.displaySmall.fontFamily,
                        fontSize = 34.sp * settings.fontScale,
                        lineHeight = 40.sp * settings.fontScale,
                        color = scheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    cursorBrush = SolidColor(accent.primary),
                    decorationBox = { inner ->
                        Box {
                            if (title.text.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.editor_untitled),
                                    style = MaterialTheme.typography.displaySmall.copy(
                                        fontFamily = if (settings.serifTitles) Fraunces else MaterialTheme.typography.displaySmall.fontFamily,
                                        fontSize = 34.sp * settings.fontScale,
                                        lineHeight = 40.sp * settings.fontScale,
                                        fontWeight = FontWeight.SemiBold,
                                    ),
                                    color = scheme.onSurface.copy(alpha = 0.28f),
                                )
                            }
                            inner()
                        }
                    },
                )

                Spacer(Modifier.height(12.dp))

                if (draft.tags.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        draft.tags.forEachIndexed { index, tag ->
                            val pop by rememberPopScale(tag)
                            Reveal(index = index) {
                                TagChip(
                                    text = "#$tag",
                                    color = FolioSchemes.tagColor(tag, dark),
                                    modifier = Modifier.graphicsLayer {
                                        scaleX = pop
                                        scaleY = pop
                                    },
                                    onClick = {
                                        draft = draft.copy(tags = draft.tags - tag)
                                        haptic(Haptic.Tap)
                                    },
                                )
                            }
                        }
                        TagChip(
                            text = stringResource(R.string.editor_add_tag_chip),
                            color = scheme.onSurfaceVariant,
                            onClick = { tagDialog = true },
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                }

                AnimatedVisibility(
                    visible = tintOpen,
                    // The tint shelf drops in from behind the header and retreats the
                    // way it came. No scaling: paper swatches that grow look like
                    // buttons being inflated, not a drawer opening.
                    enter = fadeIn(motion.enter(180)) +
                        slideInVertically(motion.pan(320)) { -it / 3 },
                    exit = fadeOut(motion.enter(140)) +
                        slideOutVertically(motion.pan(260)) { -it / 4 },
                ) {
                    Column(Modifier.padding(bottom = 12.dp)) {
                        SectionLabel(stringResource(R.string.editor_paper))
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(9.dp),
                        ) {
                            TintDot(selected = draft.tint == -1, color = scheme.surfaceContainerLow) {
                                draft = draft.copy(tint = -1)
                            }
                            FolioSchemes.noteTints(dark).forEachIndexed { index, color ->
                                TintDot(selected = draft.tint == index, color = color) {
                                    draft = draft.copy(tint = index)
                                }
                            }
                        }
                    }
                }

                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(scheme.outlineVariant.copy(alpha = 0.5f)),
                )

                Spacer(Modifier.height(16.dp))

                AnimatedVisibility(
                    visible = !preview,
                    // Write and preview are two faces of one page: the face leaving
                    // lifts, the face arriving rises. A scale swap on a full-width
                    // text column resamples the whole block for no spatial meaning.
                    enter = fadeIn(motion.enter(180)) +
                        slideInVertically(motion.pan(300)) { it / 18 },
                    exit = fadeOut(motion.enter(120)) +
                        slideOutVertically(motion.pan(220)) { -it / 20 },
                ) {
                    Column {
                        BasicTextField(
                            value = body,
                            onValueChange = ::updateBody,
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(bodyFocus)
                                .height(420.dp),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                color = scheme.onSurface.copy(alpha = 0.92f),
                                lineHeight = 27.sp * settings.fontScale,
                                fontSize = 15.5.sp * settings.fontScale,
                            ),
                            cursorBrush = SolidColor(accent.primary),
                            decorationBox = { inner ->
                                Box {
                                    if (body.text.isEmpty()) {
                                        Text(
                                            text = stringResource(R.string.editor_body_placeholder),
                                            style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 27.sp),
                                            color = scheme.onSurfaceVariant.copy(alpha = 0.42f),
                                        )
                                    }
                                    inner()
                                }
                            },
                        )
                    }
                }

                AnimatedVisibility(
                    visible = preview,
                    // Write and preview are two faces of one page: the face leaving
                    // lifts, the face arriving rises. A scale swap on a full-width
                    // text column resamples the whole block for no spatial meaning.
                    enter = fadeIn(motion.enter(180)) +
                        slideInVertically(motion.pan(300)) { it / 18 },
                    exit = fadeOut(motion.enter(120)) +
                        slideOutVertically(motion.pan(220)) { -it / 20 },
                ) {
                    Column {
                        if (draft.body.isBlank()) {
                            Text(
                                text = stringResource(R.string.editor_preview_empty),
                                style = MaterialTheme.typography.bodyMedium,
                                color = scheme.onSurfaceVariant.copy(alpha = 0.6f),
                            )
                        } else {
                            MarkdownBody(
                                markdown = draft.body,
                                onToggleTask = { line ->
                                    val next = MarkdownLite.toggleCheckbox(draft.body, line)
                                    body = TextFieldValue(next, TextRange(next.length))
                                    draft = draft.copy(body = next)
                                    haptic(Haptic.Toggle)
                                },
                            )
                        }
                    }
                }
            }
        }

        // Floating markdown toolbar, kept above the keyboard.
        val toolbarShape = RoundedCornerShape((26 * settings.cornerScale).dp)
        GlassPanel(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            shape = toolbarShape,
            elevation = 14.dp,
        ) {
            if (preview) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FolioMark(Modifier.size(18.dp), color = accent.primary)
                    Spacer(Modifier.width(10.dp))
                    val wordsLabel = pluralStringResource(
                        R.plurals.editor_stats_words,
                        draftStats.words,
                        draftStats.words,
                    )
                    val minutesLabel = pluralStringResource(
                        R.plurals.editor_read_minutes,
                        draftStats.minutes,
                        draftStats.minutes,
                    )
                    Text(
                        text = stringResource(R.string.editor_stats_format, wordsLabel, minutesLabel),
                        style = MaterialTheme.typography.labelMedium,
                        color = scheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.weight(1f))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(accent.primary.copy(alpha = 0.16f))
                            .clickableNoRipple {
                                haptic(Haptic.Tap)
                                preview = false
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.VisibilityOff,
                            contentDescription = null,
                            tint = accent.primary,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.editor_edit),
                            style = MaterialTheme.typography.labelMedium,
                            color = accent.primary,
                        )
                    }
                }
            } else {
                Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ToolButton("H1") {
                            apply(EditorOps.toggleLinePrefix(body.text, body.selection.start, body.selection.end, "# "))
                        }
                        ToolButton("H2") {
                            apply(EditorOps.toggleLinePrefix(body.text, body.selection.start, body.selection.end, "## "))
                        }
                        ToolButton(icon = Icons.Rounded.FormatBold) {
                            apply(EditorOps.wrap(body.text, body.selection.start, body.selection.end, "**"))
                        }
                        ToolButton(icon = Icons.Rounded.FormatItalic) {
                            apply(EditorOps.wrap(body.text, body.selection.start, body.selection.end, "_"))
                        }
                        ToolButton(icon = Icons.AutoMirrored.Rounded.FormatListBulleted) {
                            apply(EditorOps.toggleLinePrefix(body.text, body.selection.start, body.selection.end, "- "))
                        }
                        ToolButton(icon = Icons.Rounded.Checklist) {
                            apply(EditorOps.toggleTask(body.text, body.selection.start, body.selection.end))
                        }
                        ToolButton(icon = Icons.Rounded.FormatQuote) {
                            apply(EditorOps.toggleLinePrefix(body.text, body.selection.start, body.selection.end, "> "))
                        }
                        ToolButton(icon = Icons.Rounded.Code) {
                            apply(EditorOps.insertBlock(body.text, body.selection.start, body.selection.end, "```\n\n```"))
                        }
                        ToolButton(icon = Icons.AutoMirrored.Rounded.FormatIndentIncrease) {
                            apply(EditorOps.indent(body.text, body.selection.start, body.selection.end))
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RollingContent(target = draftStats.words) { words ->
                            Text(
                                text = pluralStringResource(R.plurals.editor_words, words, words),
                                style = MaterialTheme.typography.labelSmall,
                                color = scheme.onSurfaceVariant.copy(alpha = 0.7f),
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        val doneInteraction = remember { MutableInteractionSource() }
                        Row(
                            modifier = Modifier
                                .pressScale(doneInteraction, 0.9f)
                                .clip(RoundedCornerShape(50))
                                .background(accent.primary)
                                .clickableNoRipple(doneInteraction, true) {
                                    onPersist(draft)
                                    haptic(Haptic.Confirm)
                                    onBack()
                                }
                                .padding(horizontal = 14.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = scheme.onPrimary,
                                modifier = Modifier.size(15.dp),
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text = stringResource(R.string.action_done),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = scheme.onPrimary,
                            )
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = undoSnackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .imePadding()
                .navigationBarsPadding()
                .padding(start = 24.dp, end = 24.dp, bottom = 124.dp),
        ) { data ->
            Snackbar(
                snackbarData = data,
                containerColor = scheme.surfaceContainerHigh,
                contentColor = scheme.onSurface,
                actionContentColor = scheme.primary,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .entranceSheen(key = data)
                    .border(
                        width = 1.dp,
                        color = scheme.outlineVariant.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(20.dp),
                    ),
            )
        }
    }

    if (tagDialog) {
        FolioDialog(onDismissRequest = { tagDialog = false }) {
            FolioDialogTitle(stringResource(R.string.editor_tag_title))
            FolioDialogField(
                value = tagInput,
                onValueChange = { tagInput = it },
                placeholder = stringResource(R.string.editor_tag_placeholder),
            )
            FolioDialogActions(
                confirmLabel = stringResource(R.string.action_add),
                onConfirm = {
                    val clean = tagInput.trim().removePrefix("#")
                    if (clean.isNotEmpty() && draft.tags.none { it.equals(clean, true) }) {
                        draft = draft.copy(tags = draft.tags + clean)
                    }
                    tagInput = ""
                    tagDialog = false
                },
                dismissLabel = stringResource(R.string.action_cancel),
                onDismiss = { tagDialog = false },
            )
        }
    }

    if (templatesOpen) {
        val templateEntries = listOf(
            stringResource(R.string.template_daily_name) to stringResource(R.string.template_daily_body),
            stringResource(R.string.template_meeting_name) to stringResource(R.string.template_meeting_body),
            stringResource(R.string.template_reading_name) to stringResource(R.string.template_reading_body),
        )
        FolioDialog(onDismissRequest = { templatesOpen = false }) {
            FolioDialogTitle(stringResource(R.string.templates_title))
            Column(
                modifier = Modifier.padding(top = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                templateEntries.forEach { (name, content) ->
                    val rowInteraction = remember { MutableInteractionSource() }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .pressTilt(
                                interactionSource = rowInteraction,
                                pressedScale = 0.975f,
                                tiltDegrees = 3f,
                                cornerRadius = 14.dp,
                            )
                            .clip(RoundedCornerShape(14.dp))
                            .background(scheme.surfaceContainerHigh.copy(alpha = 0.7f))
                            .clickableNoRipple(rowInteraction, true) {
                                val inserted = EditorOps.insertTemplate(
                                    title = draft.title,
                                    body = draft.body,
                                    templateName = name,
                                    templateBody = content,
                                )
                                draft = draft.copy(title = inserted.title, body = inserted.body)
                                title = TextFieldValue(
                                    inserted.title,
                                    TextRange(inserted.title.length),
                                )
                                body = TextFieldValue(
                                    inserted.body,
                                    TextRange(inserted.body.length),
                                )
                                templatesOpen = false
                                haptic(Haptic.Confirm)
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodyLarge,
                            color = scheme.onSurface,
                        )
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = { templatesOpen = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        }
    }

    if (findOpen) {
        val found = remember(draft.body, findQuery, matchCase) {
            EditorOps.matchRanges(draft.body, findQuery, matchCase)
        }
        val current = if (found.isEmpty()) -1 else findIndex.coerceIn(-1, found.lastIndex)
        val jump: (Int) -> Unit = { step ->
            if (found.isNotEmpty()) {
                val next = if (current < 0) {
                    if (step < 0) found.lastIndex else 0
                } else {
                    (current + step + found.size) % found.size
                }
                findIndex = next
                val range = found[next]
                body = body.copy(selection = TextRange(range.start, range.end))
                preview = false
                findJumped = true
                haptic(Haptic.Tap)
            }
        }
        val matchPreview = remember(found, current, draft.body) {
            if (current < 0 || found.isEmpty()) {
                null
            } else {
                val range = found[current]
                val lineStart = EditorOps.lineStart(draft.body, range.start)
                val lineEnd = EditorOps.lineEnd(draft.body, range.start)
                val raw = draft.body.substring(lineStart, lineEnd)
                val leading = raw.indexOfFirst { !it.isWhitespace() }.takeIf { it >= 0 } ?: 0
                val line = raw.trim()
                val matchStart = (range.start - lineStart - leading).coerceIn(0, line.length)
                val matchEnd = (range.end - lineStart - leading).coerceIn(matchStart, line.length)
                val width = 96
                val windowStart = if (line.length <= width) {
                    0
                } else {
                    (matchStart - 32).coerceIn(0, line.length - width)
                }
                val windowEnd = minOf(line.length, maxOf(windowStart + width, matchEnd))
                val prefix = if (windowStart > 0) "…" else ""
                val suffix = if (windowEnd < line.length) "…" else ""
                val snippet = prefix + line.substring(windowStart, windowEnd) + suffix
                val highlightStart = prefix.length +
                    (matchStart - windowStart).coerceIn(0, windowEnd - windowStart)
                val highlightEnd = prefix.length +
                    (matchEnd - windowStart).coerceIn(0, windowEnd - windowStart)
                snippet to TextRange(highlightStart, highlightEnd)
            }
        }
        val replaceCurrent: () -> Unit = {
            val index = if (current >= 0) current else 0
            val target = found.getOrNull(index)
            if (target != null) {
                val step = EditorOps.replaceMatch(
                    text = draft.body,
                    query = findQuery,
                    replacement = replaceWith,
                    matchCase = matchCase,
                    index = index,
                )
                if (step != null) {
                    val nextRanges = EditorOps.matchRanges(step.text, findQuery, matchCase)
                    val nextRange = nextRanges.getOrNull(step.nextIndex)
                    val caret = (target.start + replaceWith.length).coerceIn(0, step.text.length)
                    val nextSelection = nextRange?.let { TextRange(it.start, it.end) }
                        ?: TextRange(caret)
                    replaceHistory = (replaceHistory + ReplaceUndo(
                        before = ReplaceState(draft.body, body.selection, findIndex, preview),
                        after = ReplaceState(step.text, nextSelection, step.nextIndex, false),
                    )).takeLast(MAX_REPLACE_HISTORY)
                    replaceRedo = emptyList()
                    body = TextFieldValue(step.text, nextSelection)
                    draft = draft.copy(body = step.text)
                    findIndex = step.nextIndex
                    preview = false
                    findJumped = true
                    haptic(Haptic.Confirm)
                }
            }
        }
        val undoReplace: () -> Unit = {
            replaceHistory.lastOrNull()?.let { restoreReplace(it) }
        }
        val redoLast: () -> Unit = {
            replaceRedo.lastOrNull()?.let { redoReplace(it) }
        }
        FolioDialog(onDismissRequest = { findOpen = false }) {
            FolioDialogTitle(stringResource(R.string.editor_find_replace))
            Rise(index = 0) {
                FolioDialogField(
                    value = findQuery,
                    onValueChange = {
                        findQuery = it
                        findIndex = -1
                        findJumped = false
                    },
                    placeholder = stringResource(R.string.editor_find_hint),
                )
            }
            Rise(index = 1) {
                FolioDialogField(
                    value = replaceWith,
                    onValueChange = { replaceWith = it },
                    placeholder = stringResource(R.string.editor_replace_hint),
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AnimatedContent(
                    targetState = found.size,
                    transitionSpec = {
                        (fadeIn(motion.enter(120)) +
                            slideInVertically(motion.pan(220)) { it / 3 }) togetherWith
                            fadeOut(motion.enter(90))
                    },
                    label = "matchCount",
                ) { count ->
                    Text(
                        text = if (findQuery.isEmpty()) "" else pluralStringResource(
                            R.plurals.editor_match_count,
                            count,
                            count,
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (count == 0) {
                            scheme.error.copy(alpha = 0.85f)
                        } else {
                            scheme.onSurfaceVariant
                        },
                    )
                }
                Spacer(Modifier.weight(1f))
                TextButton(
                    onClick = {
                        matchCase = !matchCase
                        findIndex = -1
                        findJumped = false
                    },
                ) {
                    Text(
                        text = stringResource(
                            if (matchCase) R.string.editor_match_case_on else R.string.editor_match_case,
                        ),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = { jump(-1) },
                    enabled = found.isNotEmpty(),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowUp,
                        contentDescription = stringResource(R.string.editor_previous_match),
                        modifier = Modifier.size(20.dp),
                    )
                }
                Text(
                    text = if (current < 0) "" else stringResource(
                        R.string.editor_match_position,
                        current + 1,
                        found.size,
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
                TextButton(
                    onClick = { jump(1) },
                    enabled = found.isNotEmpty(),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = stringResource(R.string.editor_next_match),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            matchPreview?.let { (line, range) ->
                val annotated = remember(line, range, scheme.primary) {
                    buildAnnotatedString {
                        append(line)
                        if (!range.collapsed) {
                            addStyle(
                                SpanStyle(
                                    color = scheme.primary,
                                    fontWeight = FontWeight.SemiBold,
                                    background = scheme.primary.copy(alpha = 0.14f),
                                ),
                                range.start,
                                range.end,
                            )
                        }
                    }
                }
                Text(
                    text = annotated,
                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                    color = scheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(scheme.surfaceContainerHigh.copy(alpha = 0.5f))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                AnimatedVisibility(
                    visible = replaceHistory.isNotEmpty(),
                    enter = fadeIn(motion.enter(140)) +
                        slideInVertically(motion.pop()) { it / 3 },
                    exit = fadeOut(motion.enter(90)),
                ) {
                    TextButton(onClick = undoReplace) {
                        Text(
                            text = stringResource(R.string.editor_undo_replace),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
                AnimatedVisibility(
                    visible = replaceRedo.isNotEmpty(),
                    enter = fadeIn(motion.enter(140)) +
                        slideInVertically(motion.pop()) { it / 3 },
                    exit = fadeOut(motion.enter(90)),
                ) {
                    TextButton(onClick = redoLast) {
                        Text(
                            text = stringResource(R.string.editor_redo_replace),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
                TextButton(
                    onClick = replaceCurrent,
                    enabled = found.isNotEmpty(),
                ) {
                    Text(
                        text = stringResource(R.string.editor_replace_match),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
            FolioDialogActions(
                confirmLabel = stringResource(R.string.editor_replace_all),
                confirmEnabled = found.isNotEmpty(),
                onConfirm = {
                    val result = EditorOps.replacement(draft.body, findQuery, replaceWith, matchCase)
                    if (result.count > 0) {
                        val afterSelection = TextRange(result.text.length)
                        replaceHistory = (replaceHistory + ReplaceUndo(
                            before = ReplaceState(draft.body, body.selection, findIndex, preview),
                            after = ReplaceState(result.text, afterSelection, -1, false),
                        )).takeLast(MAX_REPLACE_HISTORY)
                        replaceRedo = emptyList()
                        body = TextFieldValue(result.text, afterSelection)
                        draft = draft.copy(body = result.text)
                        findIndex = -1
                        preview = false
                        findJumped = true
                        haptic(Haptic.Confirm)
                    }
                },
                dismissLabel = stringResource(R.string.action_done),
                onDismiss = { findOpen = false },
            )
        }
    }
}

/** Snapshot for one reversible find-and-replace edit. */
private data class ReplaceUndo(
    val before: ReplaceState,
    val after: ReplaceState,
)

private data class ReplaceState(
    val text: String,
    val selection: TextRange,
    val findIndex: Int,
    val preview: Boolean,
)

private const val MAX_REPLACE_HISTORY = 20

/** Sends the live draft out as portable Markdown through the system share sheet. */
private fun shareNote(context: Context, note: Note) {
    val markdown = NoteExport.markdown(note, untitled = context.getString(R.string.editor_untitled))
    val subject = note.title.ifBlank { context.getString(R.string.editor_untitled) }
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, markdown)
    }
    runCatching {
        context.startActivity(Intent.createChooser(send, context.getString(R.string.share_note)))
    }
}

/** Compares what the editor can change; `updatedAt` is restamped on every save. */
private fun Note.sameContentAs(other: Note): Boolean =
    title == other.title &&
        body == other.body &&
        tags == other.tags &&
        tint == other.tint &&
        pinned == other.pinned &&
        archived == other.archived &&
        trashed == other.trashed

@Composable
private fun ToolButton(
    label: String? = null,
    icon: ImageVector? = null,
    onClick: () -> Unit,
) {
    val accent = LocalAccent.current.primary
    val interaction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    var taps by remember { mutableIntStateOf(0) }
    val pop by rememberPopScale(taps)
    Box(
        modifier = Modifier
            .size(42.dp)
            .pressTilt(
                interactionSource = interaction,
                pressedScale = 0.9f,
                tiltDegrees = 3.5f,
                cornerRadius = 13.dp,
            )
            .clip(RoundedCornerShape(13.dp))
            .clickableNoRipple(interaction, true) {
                taps++
                onClick()
            },
        contentAlignment = Alignment.Center,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(19.dp)
                    .graphicsLayer {
                        scaleX = pop
                        scaleY = pop
                    },
            )
        } else {
            Text(
                text = label.orEmpty(),
                style = TextStyle(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = accent,
                ),
                modifier = Modifier.graphicsLayer {
                    scaleX = pop
                    scaleY = pop
                },
            )
        }
    }
}

@Composable
private fun EditorMenuItem(
    label: String,
    icon: ImageVector,
    danger: Boolean = false,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text = {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            )
        },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(19.dp),
            )
        },
        onClick = onClick,
    )
}

@Composable
private fun TintDot(selected: Boolean, color: Color, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val motion = LocalFolioMotion.current
    val interaction = remember { MutableInteractionSource() }
    val bump by animateFloatAsState(
        targetValue = if (selected) 1.14f else 1f,
        animationSpec = motion.bounce(0.3f, 1100f),
        label = "tintBump",
    )
    Box(
        modifier = Modifier
            .graphicsLayer {
                scaleX = bump
                scaleY = bump
            }
            .size(38.dp)
            .pressScale(interaction, 0.82f)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) scheme.primary else scheme.outlineVariant.copy(alpha = 0.8f),
                shape = CircleShape,
            )
            .clickableNoRipple(interaction, true, onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = scheme.onSurface.copy(alpha = 0.8f),
                modifier = Modifier
                    .size(16.dp)
                    .graphicsLayer {
                        scaleX = bump.coerceAtMost(1f)
                        scaleY = bump.coerceAtMost(1f)
                    },
            )
        }
    }
}
