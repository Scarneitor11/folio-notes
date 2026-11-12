package com.folio.notes.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Sell
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.folio.notes.R
import com.folio.notes.data.Feed
import com.folio.notes.data.Note
import com.folio.notes.data.SortMode
import com.folio.notes.logic.HomeLayout
import com.folio.notes.logic.NoteQuery
import com.folio.notes.ui.components.AuroraBackground
import com.folio.notes.ui.components.EmptyState
import com.folio.notes.ui.components.FeedPills
import com.folio.notes.ui.components.FolioMark
import com.folio.notes.ui.components.MiniIconButton
import com.folio.notes.ui.components.NewNoteFab
import com.folio.notes.ui.components.NoteCard
import com.folio.notes.ui.components.Reveal
import com.folio.notes.ui.components.Rise
import com.folio.notes.ui.components.RollingContent
import com.folio.notes.ui.components.SearchField
import com.folio.notes.ui.components.SortButton
import com.folio.notes.ui.components.TagStrip
import com.folio.notes.ui.components.clickableNoRipple
import com.folio.notes.ui.theme.Fraunces
import com.folio.notes.ui.theme.LocalAccent
import com.folio.notes.ui.theme.LocalFolioMotion
import com.folio.notes.ui.theme.LocalFolioSettings

@Composable
fun HomeScreen(
    notes: List<Note>,
    feed: Feed,
    onFeedChange: (Feed) -> Unit,
    query: String,
    onQueryChange: (String) -> Unit,
    tag: String?,
    onTagChange: (String?) -> Unit,
    sort: SortMode,
    onSortChange: (SortMode) -> Unit,
    onOpen: (Note) -> Unit,
    onNew: () -> Unit,
    onLongPress: (Note) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenTags: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarVisible: Boolean = false,
    /** Where each tile sits in the window, so the editor can unfold out of it. */
    onCardBounds: (Note, Rect) -> Unit = { _, _ -> },
    selecting: Boolean = false,
    selectedIds: Set<String> = emptySet(),
    onStartSelecting: () -> Unit = {},
    onToggleSelected: (String) -> Unit = {},
    onCancelSelection: () -> Unit = {},
    onBulkPin: () -> Unit = {},
    onBulkArchive: () -> Unit = {},
    onBulkTrash: () -> Unit = {},
) {
    val settings = LocalFolioSettings.current
    val accent = LocalAccent.current
    val motion = LocalFolioMotion.current
    val scheme = MaterialTheme.colorScheme

    val visible = remember(notes, feed, sort, query, tag) {
        NoteQuery.visible(notes, feed, sort, query, tag)
    }
    val tags = remember(notes) { NoteQuery.allTags(notes) }
    val counts = remember(notes) {
        mapOf(
            Feed.ALL to notes.count { !it.trashed && !it.archived },
            Feed.PINNED to notes.count { !it.trashed && !it.archived && it.pinned },
            Feed.ARCHIVED to notes.count { !it.trashed && it.archived },
            Feed.TRASH to notes.count { it.trashed },
        )
    }
    val pinnedCount = counts[Feed.PINNED] ?: 0

    // Each feed keeps its own scroll position, and — importantly — the outgoing
    // and incoming lists never share a LazyListState while the pan crossfades.
    val gridStates = remember { mutableMapOf<Feed, LazyStaggeredGridState>() }
    val listStates = remember { mutableMapOf<Feed, LazyListState>() }
    val activate: (Note) -> Unit = { note ->
        if (selecting) onToggleSelected(note.id) else onOpen(note)
    }
    val longActivate: (Note) -> Unit = { note ->
        if (selecting) onToggleSelected(note.id) else onLongPress(note)
    }

    Box(modifier.fillMaxSize()) {
        AuroraBackground(
            modifier = Modifier.fillMaxSize(),
            accent = accent,
            blur = settings.blur,
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            Rise(index = 0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 22.dp, end = 16.dp, top = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FolioMark(modifier = Modifier.size(22.dp), color = accent.primary)
                Spacer(Modifier.width(9.dp))
                Text(
                    text = stringResource(R.string.home_wordmark),
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 3.2.sp),
                    fontWeight = FontWeight.Bold,
                    color = scheme.onSurface.copy(alpha = 0.55f),
                )
                Spacer(Modifier.weight(1f))
                MiniIconButton(
                    icon = if (selecting) Icons.Rounded.Close else Icons.Rounded.Checklist,
                    contentDescription = stringResource(
                        if (selecting) R.string.home_cancel_selection else R.string.home_select_notes,
                    ),
                    onClick = if (selecting) onCancelSelection else onStartSelecting,
                    enabled = selecting || (feed != Feed.TRASH && visible.isNotEmpty()),
                    container = scheme.surface.copy(alpha = if (settings.blur) 0.55f else 0.9f),
                )
                Spacer(Modifier.width(8.dp))
                MiniIconButton(
                    icon = Icons.Rounded.Sell,
                    contentDescription = stringResource(R.string.home_open_tags),
                    onClick = onOpenTags,
                    container = scheme.surface.copy(alpha = if (settings.blur) 0.55f else 0.9f),
                )
                Spacer(Modifier.width(8.dp))
                MiniIconButton(
                    icon = Icons.Rounded.Settings,
                    contentDescription = stringResource(R.string.home_open_settings),
                    onClick = onOpenSettings,
                    container = scheme.surface.copy(alpha = if (settings.blur) 0.55f else 0.9f),
                )
            }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 22.dp, end = 22.dp, top = 18.dp),
            ) {
                AnimatedContent(
                    targetState = feed,
                    transitionSpec = {
                        // The feed title is a page heading, not a thing you flip to:
                        // it rises a little and the old one leaves upward, no scale.
                        (fadeIn(tween(motion.millis(200))) +
                            slideInVertically(tween(motion.millis(260))) { it / 4 }) togetherWith
                            (fadeOut(tween(motion.millis(110))) +
                                slideOutVertically(tween(motion.millis(160))) { -it / 8 })
                    },
                    label = "feedTitle",
                ) { shownFeed ->
                    Text(
                        text = when (shownFeed) {
                            Feed.ALL -> stringResource(R.string.home_title_all)
                            Feed.PINNED -> stringResource(R.string.home_title_pinned)
                            Feed.ARCHIVED -> stringResource(R.string.home_title_archived)
                            Feed.TRASH -> stringResource(R.string.home_title_trash)
                        },
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontFamily = if (settings.serifTitles) Fraunces else MaterialTheme.typography.displaySmall.fontFamily,
                        ),
                        color = scheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(4.dp))
                val notesLabel = pluralStringResource(R.plurals.note_count, visible.size, visible.size)
                val pinnedLabel = if (feed == Feed.ALL && pinnedCount > 0) {
                    pluralStringResource(R.plurals.home_pinned_count, pinnedCount, pinnedCount)
                } else {
                    null
                }
                val metaLabel = buildString {
                    append(notesLabel)
                    if (pinnedLabel != null) {
                        append("  ·  ")
                        append(pinnedLabel)
                    }
                    if (tag != null) {
                        append("  ·  #")
                        append(tag)
                    }
                }
                RollingContent(
                    target = metaLabel,
                    direction = { _, _ -> 1 },
                ) { label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant.copy(alpha = 0.85f),
                        maxLines = 1,
                    )
                }

                Spacer(Modifier.height(18.dp))
                if (selecting) {
                    SelectionToolbar(
                        count = selectedIds.size,
                        archived = feed == Feed.ARCHIVED,
                        onPin = onBulkPin,
                        onArchive = onBulkArchive,
                        onTrash = onBulkTrash,
                    )
                } else {
                    Rise(index = 2) { SearchField(value = query, onValueChange = onQueryChange) }
                    Spacer(Modifier.height(14.dp))
                    Rise(index = 3) {
                        FeedPills(feed = feed, onChange = onFeedChange, counts = counts)
                    }
                    if (tags.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        Rise(index = 4) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TagStrip(
                                    tags = tags,
                                    selected = tag,
                                    onSelect = onTagChange,
                                    modifier = Modifier.weight(1f),
                                )
                                Spacer(Modifier.width(8.dp))
                                SortButton(sort = sort, onChange = onSortChange)
                            }
                        }
                    } else {
                        Rise(index = 4) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                SortButton(sort = sort, onChange = onSortChange)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
            }

            Box(Modifier.weight(1f).fillMaxWidth()) {
                AnimatedContent(
                    targetState = feed,
                    transitionSpec = {
                        // Feeds sit in a fixed order, so moving to a later feed slides
                        // right-to-left and moving back mirrors it.
                        val dir = if (targetState.ordinal >= initialState.ordinal) 1 else -1
                        (slideInHorizontally(tween(motion.millis(300))) { width -> dir * width / 7 } +
                            fadeIn(tween(motion.millis(190)))) togetherWith
                            (slideOutHorizontally(tween(motion.millis(150))) { width -> -dir * width / 11 } +
                                fadeOut(tween(motion.millis(130))))
                    },
                    label = "feedBody",
                    modifier = Modifier.fillMaxSize(),
                ) { shownFeed ->
                    val gridState = remember(shownFeed) {
                        gridStates.getOrPut(shownFeed) { LazyStaggeredGridState() }
                    }
                    val listState = remember(shownFeed) {
                        listStates.getOrPut(shownFeed) { LazyListState() }
                    }
                    // The outgoing feed keeps its own rows during the pan instead of
                    // double-drawing the incoming filter.
                    val rows = remember(notes, shownFeed, sort, query, tag) {
                        if (shownFeed == feed) {
                            visible
                        } else {
                            NoteQuery.visible(notes, shownFeed, sort, query, tag)
                        }
                    }
                    if (rows.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                            EmptyState(
                                title = when {
                                    query.isNotBlank() -> stringResource(R.string.home_empty_found)
                                    shownFeed == Feed.TRASH -> stringResource(R.string.home_empty_trash)
                                    shownFeed == Feed.ARCHIVED -> stringResource(R.string.home_empty_archived)
                                    shownFeed == Feed.PINNED -> stringResource(R.string.home_empty_pinned)
                                    tag != null -> stringResource(R.string.home_empty_tagged, tag)
                                    else -> stringResource(R.string.home_empty_blank)
                                },
                                message = when {
                                    query.isNotBlank() -> stringResource(R.string.home_empty_found_message, query)
                                    shownFeed == Feed.TRASH -> stringResource(R.string.home_empty_trash_message)
                                    shownFeed == Feed.ARCHIVED -> stringResource(R.string.home_empty_archived_message)
                                    shownFeed == Feed.PINNED -> stringResource(R.string.home_empty_pinned_message)
                                    tag != null -> stringResource(R.string.home_empty_tagged_message, tag)
                                    else -> stringResource(R.string.home_empty_blank_message)
                                },
                                actionLabel = if (shownFeed == Feed.ALL && query.isBlank()) {
                                    stringResource(R.string.home_write_note)
                                } else {
                                    null
                                },
                                onAction = if (shownFeed == Feed.ALL && query.isBlank()) onNew else null,
                            )
                        }
                    } else {
                        // The grid widens with the window (two-up on a phone, four-up on a
                        // tablet) unless the count is pinned in Settings. The list stays a
                        // readable column instead of stretching edge to edge.
                        BoxWithConstraints(Modifier.fillMaxSize()) {
                            val columns = HomeLayout.gridColumns(
                                availableWidthDp = maxWidth.value - 40f,
                                preference = settings.gridColumns,
                            )
                            val narrowCards = HomeLayout.isNarrowGrid(
                                availableWidthDp = maxWidth.value,
                                columns = columns,
                            )
                            if (settings.grid) {
                                LazyVerticalStaggeredGrid(
                                    columns = StaggeredGridCells.Fixed(columns),
                                    state = gridState,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        // Reserve the band the floating button lives in so
                                        // no card is ever half-hidden behind it at rest.
                                        .padding(bottom = 104.dp),
                                    contentPadding = PaddingValues(
                                        start = 20.dp,
                                        end = 20.dp,
                                        top = 10.dp,
                                        bottom = 28.dp,
                                    ),
                                    verticalItemSpacing = 12.dp,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    itemsIndexed(rows, key = { _, note -> note.id }) { index, note ->
                                        Reveal(
                                            index = index,
                                            modifier = Modifier.animateItem(
                                                fadeInSpec = null,
                                                fadeOutSpec = tween(motion.millis(140)),
                                                placementSpec = spring(
                                                    dampingRatio = 1f,
                                                    stiffness = 900f,
                                                ),
                                            ),
                                        ) {
                                            NoteCard(
                                                note = note,
                                                index = index,
                                                narrow = narrowCards,
                                                selected = selecting && note.id in selectedIds,
                                                onClick = { activate(note) },
                                                onLongClick = { longActivate(note) },
                                                onBounds = { onCardBounds(note, it) },
                                            )
                                        }
                                    }
                                }
                            } else {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                                    LazyColumn(
                                        state = listState,
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .widthIn(max = HomeLayout.LIST_MAX_WIDTH_DP.dp)
                                            .fillMaxWidth()
                                            .padding(bottom = 104.dp),
                                        contentPadding = PaddingValues(
                                            start = 20.dp,
                                            end = 20.dp,
                                            top = 10.dp,
                                            bottom = 28.dp,
                                        ),
                                        verticalArrangement = Arrangement.spacedBy(11.dp),
                                    ) {
                                        itemsIndexed(rows, key = { _, note -> note.id }) { index, note ->
                                            Reveal(
                                                index = index,
                                                modifier = Modifier.animateItem(
                                                    fadeInSpec = null,
                                                    fadeOutSpec = tween(motion.millis(140)),
                                                    placementSpec = spring(
                                                        dampingRatio = 1f,
                                                        stiffness = 900f,
                                                    ),
                                                ),
                                            ) {
                                                NoteCard(
                                                    note = note,
                                                    index = index,
                                                    compact = false,
                                                    selected = selecting && note.id in selectedIds,
                                                    onClick = { activate(note) },
                                                    onLongClick = { longActivate(note) },
                                                    onBounds = { onCardBounds(note, it) },
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // A snackbar lands in the bottom band; lift the button out of its way instead of
        // letting the two stack on top of each other.
        val fabLift by animateDpAsState(
            targetValue = if (snackbarVisible) 92.dp else 22.dp,
            animationSpec = spring(dampingRatio = 1f, stiffness = 420f),
            label = "fabLift",
        )
        var fabShown by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { fabShown = true }
        val fabPop by animateFloatAsState(
            targetValue = if (fabShown) 1f else 0f,
            animationSpec = motion.bounce(0.45f, 420f),
            label = "fabEnter",
        )
        if (!selecting) {
            NewNoteFab(
                onClick = onNew,
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = fabPop
                        scaleY = fabPop
                        alpha = fabPop.coerceIn(0f, 1f)
                    }
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 20.dp, bottom = fabLift),
            )
        }
    }
}

@Composable
private fun SelectionToolbar(
    count: Int,
    archived: Boolean,
    onPin: () -> Unit,
    onArchive: () -> Unit,
    onTrash: () -> Unit,
) {
    val settings = LocalFolioSettings.current
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape((18 * settings.cornerScale).dp)
    val enabled = count > 0
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(scheme.surfaceContainerLow.copy(alpha = 0.88f))
            .border(1.dp, scheme.outlineVariant.copy(alpha = 0.45f), shape)
            .padding(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = pluralStringResource(R.plurals.home_selected_count, count, count),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = scheme.onSurface,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = stringResource(R.string.home_selection_hint),
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant.copy(alpha = 0.7f),
            )
        }
        Spacer(Modifier.height(7.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            SelectionAction(
                icon = Icons.Rounded.PushPin,
                label = stringResource(R.string.home_bulk_pin),
                enabled = enabled,
                onClick = onPin,
                modifier = Modifier.weight(1f),
            )
            SelectionAction(
                icon = if (archived) Icons.Rounded.Unarchive else Icons.Rounded.Archive,
                label = stringResource(
                    if (archived) R.string.home_bulk_unarchive else R.string.home_bulk_archive,
                ),
                enabled = enabled,
                onClick = onArchive,
                modifier = Modifier.weight(1f),
            )
            SelectionAction(
                icon = Icons.Rounded.Delete,
                label = stringResource(R.string.home_bulk_trash),
                enabled = enabled,
                danger = true,
                onClick = onTrash,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SelectionAction(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    danger: Boolean = false,
) {
    val scheme = MaterialTheme.colorScheme
    val interaction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val tint = (if (danger) scheme.error else scheme.onSurfaceVariant)
        .copy(alpha = if (enabled) 1f else 0.35f)
    Row(
        modifier = modifier
            .heightIn(min = 42.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(scheme.surfaceContainerHigh.copy(alpha = 0.62f))
            .clickableNoRipple(interaction, enabled, onClick)
            .padding(horizontal = 7.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = tint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
