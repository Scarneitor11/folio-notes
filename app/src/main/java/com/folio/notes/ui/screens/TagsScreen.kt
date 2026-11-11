package com.folio.notes.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DriveFileRenameOutline
import androidx.compose.material.icons.rounded.Sell
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.folio.notes.R
import com.folio.notes.logic.FolioSchemes
import com.folio.notes.ui.components.AuroraBackground
import com.folio.notes.ui.components.FolioDialog
import com.folio.notes.ui.components.FolioDialogActions
import com.folio.notes.ui.components.FolioDialogBody
import com.folio.notes.ui.components.FolioDialogField
import com.folio.notes.ui.components.FolioDialogTitle
import com.folio.notes.ui.components.FolioMark
import com.folio.notes.ui.components.GlassPanel
import com.folio.notes.ui.components.MiniIconButton
import com.folio.notes.ui.components.Reveal
import com.folio.notes.ui.components.Rise
import com.folio.notes.ui.components.RollingContent
import com.folio.notes.ui.components.SectionLabel
import com.folio.notes.ui.components.clickableNoRipple
import com.folio.notes.ui.components.pressScale
import com.folio.notes.ui.components.pressTilt
import com.folio.notes.ui.components.rememberHaptics
import com.folio.notes.ui.components.Haptic
import com.folio.notes.ui.theme.LocalAccent
import com.folio.notes.ui.theme.LocalFolioDark
import com.folio.notes.ui.theme.LocalFolioMotion
import com.folio.notes.ui.theme.LocalFolioSettings
import com.folio.notes.ui.theme.rememberBreathingState
import androidx.compose.foundation.interaction.MutableInteractionSource

/**
 * Tag library: every tag with its note count, a colour, and inline rename and
 * delete. Tapping a tag drops back to the home feed filtered by it.
 */
@Composable
fun TagsScreen(
    tags: List<Pair<String, Int>>,
    onBack: () -> Unit,
    onSelect: (String) -> Unit,
    onRename: (String, String) -> Unit,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val settings = LocalFolioSettings.current
    val dark = LocalFolioDark.current
    val accent = LocalAccent.current
    val scheme = MaterialTheme.colorScheme
    val haptic = rememberHaptics()

    var renaming by remember { mutableStateOf<String?>(null) }
    var renameTo by remember { mutableStateOf("") }
    var deleting by remember { mutableStateOf<String?>(null) }

    Box(modifier.fillMaxSize()) {
        AuroraBackground(Modifier.fillMaxSize(), accent = accent, blur = settings.blur)

        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            Rise(index = 0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 18.dp, top = 8.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MiniIconButton(
                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.action_back),
                    onClick = onBack,
                    container = scheme.surface.copy(alpha = if (settings.blur) 0.55f else 0.9f),
                )
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.tags_section),
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.6.sp),
                        fontWeight = FontWeight.Bold,
                        color = scheme.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                    Text(
                        text = pluralStringResource(R.plurals.label_count, tags.size, tags.size),
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant.copy(alpha = 0.45f),
                    )
                }
                FolioMark(Modifier.size(24.dp), color = accent.primary.copy(alpha = 0.7f))
            }
            }

            if (tags.isEmpty()) {
                EmptyTags(accent.primary)
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = 18.dp,
                        end = 18.dp,
                        top = 6.dp,
                        bottom = 30.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    itemsIndexed(tags, key = { _, tag -> tag.first.lowercase() }) { index, (tag, count) ->
                        val color = FolioSchemes.tagColor(tag, dark)
                        val motion = LocalFolioMotion.current
                        Reveal(
                            index = index,
                            modifier = Modifier.animateItem(
                                fadeInSpec = null,
                                fadeOutSpec = tween(if (motion.reduced) 1 else 140),
                                placementSpec = spring(
                                    dampingRatio = 1f,
                                    stiffness = 760f,
                                ),
                            ),
                        ) {
                            TagRow(
                                tag = tag,
                                count = count,
                                color = color,
                                onClick = {
                                    haptic(Haptic.Tap)
                                    onSelect(tag)
                                },
                                onRename = {
                                    renaming = tag
                                    renameTo = tag
                                },
                                onDelete = { deleting = tag },
                            )
                        }
                    }
                }
            }
        }
    }

    val editing = renaming
    if (editing != null) {
        FolioDialog(onDismissRequest = { renaming = null }) {
            FolioDialogTitle(stringResource(R.string.tags_rename_title))
            FolioDialogField(
                value = renameTo,
                onValueChange = { renameTo = it },
            )
            FolioDialogActions(
                confirmLabel = stringResource(R.string.action_rename),
                onConfirm = {
                    val next = renameTo.trim().removePrefix("#")
                    if (next.isNotEmpty() && !next.equals(editing, true)) {
                        onRename(editing, next)
                        haptic(Haptic.Confirm)
                    }
                    renaming = null
                },
                dismissLabel = stringResource(R.string.action_cancel),
                onDismiss = { renaming = null },
                confirmEnabled = renameTo.trim().removePrefix("#").isNotEmpty(),
            )
        }
    }

    val doomed = deleting
    if (doomed != null) {
        FolioDialog(onDismissRequest = { deleting = null }) {
            FolioDialogTitle(stringResource(R.string.tags_remove_title, doomed))
            FolioDialogBody(stringResource(R.string.tags_remove_message))
            FolioDialogActions(
                confirmLabel = stringResource(R.string.action_remove),
                onConfirm = {
                    onDelete(doomed)
                    deleting = null
                    haptic(Haptic.Confirm)
                },
                dismissLabel = stringResource(R.string.action_keep),
                onDismiss = { deleting = null },
                destructive = true,
            )
        }
    }
}

@Composable
private fun TagRow(
    tag: String,
    count: Int,
    color: Color,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val settings = LocalFolioSettings.current
    val motion = LocalFolioMotion.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val iconScale by animateFloatAsState(
        targetValue = if (pressed) 0.72f else 1f,
        animationSpec = motion.bounce(0.26f, 1600f),
        label = "tagIcon",
    )
    val iconSpin by animateFloatAsState(
        targetValue = if (pressed) -14f else 0f,
        animationSpec = motion.bounce(0.3f, 1300f),
        label = "tagSpin",
    )
    GlassPanel(
        modifier = modifier
            .fillMaxWidth()
            .pressTilt(
                interactionSource = interaction,
                pressedScale = 0.945f,
                tiltDegrees = 4.2f,
                cornerRadius = (22 * settings.cornerScale).dp,
                sheenAlpha = 0.20f,
            )
            .clickableNoRipple(interaction, true, onClick),
        shape = RoundedCornerShape((22 * settings.cornerScale).dp),
        elevation = 4.dp,
        borderAlpha = 0.28f,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 10.dp, top = 13.dp, bottom = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.24f))
                    .border(1.dp, color.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Sell,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier
                        .size(17.dp)
                        .graphicsLayer {
                            scaleX = iconScale
                            scaleY = iconScale
                            rotationZ = iconSpin
                        },
                )
            }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = "#$tag",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = scheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                RollingContent(
                    target = pluralStringResource(R.plurals.note_count, count, count),
                    direction = { _, _ -> 1 },
                ) { label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1,
                    )
                }
            }
            MiniIconButton(
                icon = Icons.Rounded.DriveFileRenameOutline,
                contentDescription = stringResource(R.string.tags_rename_desc, tag),
                onClick = onRename,
                size = 36.dp,
                tint = scheme.onSurfaceVariant,
                container = Color.Transparent,
            )
            MiniIconButton(
                icon = Icons.Rounded.Close,
                contentDescription = stringResource(R.string.tags_delete_desc, tag),
                onClick = onDelete,
                size = 36.dp,
                tint = scheme.error.copy(alpha = 0.9f),
                container = Color.Transparent,
            )
        }
    }
}

@Composable
private fun EmptyTags(color: Color) {
    val scheme = MaterialTheme.colorScheme
    // Read only inside graphicsLayer so the slow bob never triggers recomposition.
    val float = rememberBreathingState(key = "emptyTags", periodMillis = 3800)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .graphicsLayer {
                    translationY = -6f + 12f * float.value
                    rotationZ = -3f + 6f * float.value
                }
                .clip(CircleShape)
                .background(color.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Sell,
                contentDescription = null,
                tint = color.copy(alpha = 0.8f),
                modifier = Modifier.size(38.dp),
            )
        }
        Spacer(Modifier.height(22.dp))
        Text(
            text = stringResource(R.string.tags_empty_title),
            style = MaterialTheme.typography.headlineSmall,
            color = scheme.onSurface,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.tags_empty_message),
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurfaceVariant.copy(alpha = 0.75f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}
