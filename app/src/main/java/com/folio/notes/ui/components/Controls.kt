package com.folio.notes.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Label
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.folio.notes.R
import com.folio.notes.data.Feed
import com.folio.notes.data.Note
import com.folio.notes.data.SortMode
import com.folio.notes.logic.FolioSchemes
import com.folio.notes.logic.NoteQuery
import com.folio.notes.ui.theme.Fraunces
import com.folio.notes.ui.theme.LocalAccent
import com.folio.notes.ui.theme.LocalFolioMotion
import com.folio.notes.ui.theme.rememberBreathingState
import com.folio.notes.ui.theme.LocalFolioDark
import com.folio.notes.ui.theme.LocalFolioSettings

@Composable
fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
) {
    val placeholderText = placeholder ?: stringResource(R.string.search_placeholder)
    val scheme = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val shape = RoundedCornerShape(50)
    val motion = LocalFolioMotion.current
    val focusScale by animateFloatAsState(
        targetValue = if (focused) 1.018f else 1f,
        animationSpec = motion.bounce(0.4f, 900f),
        label = "searchScale",
    )
    val iconScale by animateFloatAsState(
        targetValue = if (focused) 1.2f else 1f,
        animationSpec = motion.bounce(0.3f, 1200f),
        label = "searchIcon",
    )
    val container by animateColorAsState(
        targetValue = if (focused) scheme.surfaceContainerHigh else scheme.surfaceContainerLow,
        animationSpec = motion.bounce(0.6f, 800f),
        label = "searchBg",
    )
    Row(
        modifier = modifier
            .graphicsLayer {
                scaleX = focusScale
                scaleY = focusScale
            }
            .clip(shape)
            .background(container)
            .border(
                width = 1.dp,
                color = if (focused) scheme.primary.copy(alpha = 0.45f) else scheme.outlineVariant.copy(alpha = 0.55f),
                shape = shape,
            )
            .padding(start = 15.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.Search,
            contentDescription = null,
            tint = if (focused) scheme.primary else scheme.onSurfaceVariant.copy(alpha = 0.8f),
            modifier = Modifier
                .size(18.dp)
                .graphicsLayer {
                    scaleX = iconScale
                    scaleY = iconScale
                },
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 10.dp, vertical = 14.dp),
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = scheme.onSurface),
            singleLine = true,
            cursorBrush = SolidColor(scheme.primary),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholderText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onSurfaceVariant.copy(alpha = 0.65f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    inner()
                }
            },
        )
        androidx.compose.animation.AnimatedVisibility(
            visible = value.isNotEmpty(),
            // The clear affordance grows out of the field's own edge at a size it
            // could actually occupy, so it reads as the button arriving rather than
            // as a pop.
            enter = androidx.compose.animation.fadeIn(motion.fade(150)) +
                androidx.compose.animation.scaleIn(motion.snap(), initialScale = 0.82f),
            exit = androidx.compose.animation.fadeOut(motion.fade(110)) +
                androidx.compose.animation.scaleOut(motion.snap(), targetScale = 0.82f),
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .clickableNoRipple { onValueChange("") },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.search_clear),
                    tint = scheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
fun FeedPills(
    feed: Feed,
    onChange: (Feed) -> Unit,
    counts: Map<Feed, Int>,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val motion = LocalFolioMotion.current
    val haptic = rememberHaptics()
    val entries = listOf(
        Feed.ALL to stringResource(R.string.feed_notes),
        Feed.PINNED to stringResource(R.string.feed_pinned),
        Feed.ARCHIVED to stringResource(R.string.feed_archive),
        Feed.TRASH to stringResource(R.string.feed_trash),
    )
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(scheme.surfaceContainerLow)
            .border(1.dp, scheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(50))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        entries.forEach { (target, label) ->
            val selected = target == feed
            val container by animateColorAsState(
                targetValue = if (selected) scheme.primary else Color.Transparent,
                animationSpec = spring(dampingRatio = 1f, stiffness = 900f),
                label = "pill-$label",
            )
            val content by animateColorAsState(
                targetValue = if (selected) scheme.onPrimary else scheme.onSurfaceVariant,
                animationSpec = tween(220),
                label = "pillText-$label",
            )
            val count = counts[target] ?: 0
            val bump by animateFloatAsState(
                targetValue = if (selected) 1.05f else 1f,
                animationSpec = motion.bounce(0.35f, 1100f),
                label = "pillBump",
            )
            val selectPop by rememberPopScale(selected, from = 0.90f)
            Row(
                modifier = Modifier
                    .weight(1f)
                    .graphicsLayer {
                        scaleX = bump * selectPop
                        scaleY = bump * selectPop
                    }
                    .clip(RoundedCornerShape(50))
                    .background(container)
                    .clickableNoRipple {
                        if (!selected) {
                            haptic(Haptic.Toggle)
                            onChange(target)
                        }
                    }
                    .padding(vertical = 9.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    color = content,
                    maxLines = 1,
                )
                if (count > 0) {
                    Spacer(Modifier.width(4.dp))
                    RollingContent(
                        target = count,
                        modifier = Modifier,
                        direction = { a, b -> if (b >= a) 1 else -1 },
                    ) { value ->
                        Text(
                            text = value.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = content.copy(alpha = if (selected) 0.75f else 0.5f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TagStrip(
    tags: List<Pair<String, Int>>,
    selected: String?,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dark = LocalFolioDark.current
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 0.dp),
    ) {
        item {
            TagChip(
                text = stringResource(R.string.tag_all),
                color = MaterialTheme.colorScheme.primary,
                selected = selected == null,
                onClick = { onSelect(null) },
            )
        }
        items(tags, key = { it.first }) { (tag, count) ->
            TagChip(
                text = "$tag  $count",
                color = FolioSchemes.tagColor(tag, dark),
                selected = selected.equals(tag, ignoreCase = true),
                onClick = { onSelect(if (selected.equals(tag, true)) null else tag) },
            )
        }
    }
}

@Composable
fun SortButton(
    sort: SortMode,
    onChange: (SortMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    var open by remember { mutableStateOf(false) }
    val motion = LocalFolioMotion.current
    val interaction = remember { MutableInteractionSource() }
    val spin by animateFloatAsState(
        targetValue = if (open) 180f else 0f,
        animationSpec = motion.bounce(0.5f, 800f),
        label = "sortSpin",
    )
    Box(modifier) {
        Row(
            modifier = Modifier
                .pressTilt(
                    interactionSource = interaction,
                    pressedScale = 0.92f,
                    tiltDegrees = 10f,
                    cornerRadius = 18.dp,
                    sheenAlpha = 0.22f,
                )
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .clickableNoRipple(interaction, true) { open = true }
                .padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.Sort,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(15.dp)
                    .graphicsLayer { rotationZ = spin },
            )
            Text(
                text = sortLabel(sort),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            shape = RoundedCornerShape(18.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            SortMode.entries.forEach { mode ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = sortLabel(mode),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (mode == sort) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        )
                    },
                    onClick = {
                        onChange(mode)
                        open = false
                    },
                )
            }
        }
    }
}

@Composable
fun sortLabel(sort: SortMode): String = when (sort) {
    SortMode.UPDATED_DESC -> stringResource(R.string.sort_updated_desc)
    SortMode.UPDATED_ASC -> stringResource(R.string.sort_updated_asc)
    SortMode.CREATED_DESC -> stringResource(R.string.sort_created_desc)
    SortMode.TITLE_ASC -> stringResource(R.string.sort_title_asc)
    SortMode.TITLE_DESC -> stringResource(R.string.sort_title_desc)
}

@Composable
fun NewNoteFab(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val haptic = rememberHaptics()
    val motion = LocalFolioMotion.current
    val spin by animateFloatAsState(
        targetValue = if (pressed) 90f else 0f,
        animationSpec = motion.bounce(0.3f, 900f),
        label = "fabSpin",
    )
    Row(
        modifier = modifier
            .pressTilt(
                interactionSource = interaction,
                pressedScale = 0.93f,
                tiltDegrees = 6f,
                cornerRadius = 27.dp,
                sheenAlpha = 0.34f,
            )
            .shadow(
                elevation = 18.dp,
                shape = CircleShape,
                ambientColor = scheme.primary.copy(alpha = 0.5f),
                spotColor = scheme.primary.copy(alpha = 0.6f),
            )
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    colors = listOf(scheme.primary, scheme.tertiary),
                    start = Offset.Zero,
                    end = Offset(280f, 160f),
                ),
            )
            .border(1.dp, Color.White.copy(alpha = 0.28f), CircleShape)
            .combinedPress(interaction) {
                haptic(Haptic.Confirm)
                onClick()
            }
            .padding(horizontal = 21.dp, vertical = 17.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Icon(
            imageVector = Icons.Rounded.Add,
            contentDescription = null,
            tint = scheme.onPrimary,
            modifier = Modifier
                .size(20.dp)
                .graphicsLayer { rotationZ = spin },
        )
        Text(
            text = stringResource(R.string.fab_new_note),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = scheme.onPrimary,
        )
    }
}

private fun Modifier.combinedPress(
    interaction: MutableInteractionSource,
    onClick: () -> Unit,
): Modifier = this.clickableNoRipple(interaction, true, onClick)

@Composable
fun EmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val accent = LocalAccent.current
    // Read only inside graphicsLayer so the slow bob never triggers recomposition.
    val float = rememberBreathingState(key = "empty", periodMillis = 4200)
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(118.dp)
                .graphicsLayer {
                    translationY = -8f + 16f * float.value
                    rotationZ = -4f + 8f * float.value
                },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(30.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                accent.primary.copy(alpha = 0.22f),
                                accent.tertiary.copy(alpha = 0.18f),
                            ),
                        ),
                    ),
            )
            FolioMark(modifier = Modifier.size(46.dp), color = accent.primary.copy(alpha = 0.85f))
        }
        Spacer(Modifier.height(20.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontFamily = if (LocalFolioSettings.current.serifTitles) Fraunces else MaterialTheme.typography.headlineSmall.fontFamily,
            ),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(20.dp))
            val actionInteraction = remember { MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .pressTilt(
                        interactionSource = actionInteraction,
                        pressedScale = 0.90f,
                        tiltDegrees = 8f,
                        cornerRadius = 22.dp,
                        sheenAlpha = 0.30f,
                    )
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.primary)
                    .clickableNoRipple(actionInteraction, true, onAction)
                    .padding(horizontal = 20.dp, vertical = 11.dp),
            ) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
    }
}

enum class NoteAction { Pin, Archive, Unarchive, Trash, Restore, DeleteForever, Tag, Tint, Duplicate }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteActionSheet(
    note: Note,
    onDismiss: () -> Unit,
    onAction: (NoteAction) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scheme = MaterialTheme.colorScheme
    val haptic = rememberHaptics()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = scheme.surfaceContainerLow,
        contentColor = scheme.onSurface,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        dragHandle = {
            Box(
                Modifier
                    .padding(top = 12.dp, bottom = 6.dp)
                    .size(width = 38.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(scheme.onSurfaceVariant.copy(alpha = 0.35f)),
            )
        },
    ) {
        Column(
            Modifier
                .navigationBarsPadding()
                .padding(horizontal = 18.dp)
                .padding(bottom = 26.dp),
        ) {
            Text(
                text = NoteQuery.titleOf(note),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = if (LocalFolioSettings.current.serifTitles) Fraunces else MaterialTheme.typography.titleMedium.fontFamily,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            )
            val items = buildList {
                add(Triple(if (note.pinned) stringResource(R.string.sheet_unpin) else stringResource(R.string.sheet_pin), NoteAction.Pin, Icons.Rounded.PushPin))
                if (note.trashed) {
                    add(Triple(stringResource(R.string.sheet_restore), NoteAction.Restore, Icons.Rounded.Restore))
                    add(Triple(stringResource(R.string.sheet_delete_forever), NoteAction.DeleteForever, Icons.Rounded.DeleteForever))
                } else {
                    add(
                        Triple(
                            if (note.archived) stringResource(R.string.sheet_unarchive) else stringResource(R.string.sheet_archive),
                            if (note.archived) NoteAction.Unarchive else NoteAction.Archive,
                            Icons.Rounded.Archive,
                        ),
                    )
                    add(Triple(stringResource(R.string.sheet_add_tag), NoteAction.Tag, Icons.Rounded.Label))
                    add(Triple(stringResource(R.string.sheet_note_colour), NoteAction.Tint, Icons.Rounded.Palette))
                    add(Triple(stringResource(R.string.sheet_duplicate), NoteAction.Duplicate, Icons.Rounded.ContentCopy))
                    add(Triple(stringResource(R.string.sheet_move_to_trash), NoteAction.Trash, Icons.Rounded.Delete))
                }
            }
            items.forEachIndexed { index, (label, action, icon) ->
                val danger = action == NoteAction.DeleteForever || action == NoteAction.Trash
                val rowInteraction = remember { MutableInteractionSource() }
                Reveal(index = index) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .pressTilt(
                                interactionSource = rowInteraction,
                                pressedScale = 0.955f,
                                tiltDegrees = 3.6f,
                                cornerRadius = 16.dp,
                                sheenAlpha = 0.18f,
                            )
                            .clip(RoundedCornerShape(16.dp))
                            .clickableNoRipple(rowInteraction, true) {
                                haptic(Haptic.Tap)
                                onAction(action)
                                onDismiss()
                            }
                            .padding(horizontal = 12.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (danger) scheme.error else scheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (danger) scheme.error else scheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}
