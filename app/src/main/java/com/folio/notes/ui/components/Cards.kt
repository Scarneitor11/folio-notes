package com.folio.notes.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.folio.notes.R
import com.folio.notes.data.Note
import com.folio.notes.logic.ColorMath
import com.folio.notes.logic.FolioSchemes
import com.folio.notes.logic.NoteQuery
import com.folio.notes.ui.theme.Fraunces
import com.folio.notes.ui.theme.LocalAccent
import com.folio.notes.ui.theme.LocalFolioDark
import com.folio.notes.ui.theme.LocalFolioMotion
import com.folio.notes.ui.theme.LocalFolioSettings

/** A single note tile. Height follows its content, which is what makes the grid feel alive. */
@Composable
fun NoteCard(
    note: Note,
    index: Int,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = true,
    /**
     * True when the grid packs three or more cards per row on one screen. The card
     * then sheds the details that stop fitting - the title shrinks, the preview gets
     * fewer lines, only one tag shows and the pin moves to the footer - so a dense
     * grid stays legible instead of breaking words down the middle.
     */
    narrow: Boolean = false,
    selected: Boolean = false,
    /**
     * Reports where the card sits in the window, once, whenever it is laid out. The
     * tap handler reads that rect so the editor can grow out of this exact tile
     * instead of sliding in from the edge like any other page.
     */
    onBounds: ((Rect) -> Unit)? = null,
) {
    val settings = LocalFolioSettings.current
    val dark = LocalFolioDark.current
    val scheme = MaterialTheme.colorScheme
    val accent = LocalAccent.current
    val motion = LocalFolioMotion.current

    // Deriving the title can read and parse the body when the title field is blank.
    // Cache both derived strings per edit so scrolling never reparses a note.
    val title = remember(note.title, note.body) { NoteQuery.titleOf(note) }
    val preview = remember(note.title, note.body) { NoteQuery.previewOf(note) }
    val tint by animateColorAsState(
        targetValue = if (note.tint >= 0) FolioSchemes.tintAt(note.tint, dark) else scheme.surfaceContainerLow,
        animationSpec = motion.bounce(0.55f, 500f),
        label = "cardTint",
    )
    val wash = ColorMath.mix(tint, accent.primary, if (dark) 0.16f else 0.10f)
    val ink = scheme.onSurface
    val corner = (if (compact) 22f else 26f) * settings.cornerScale
    val shape = RoundedCornerShape(corner.dp)

    val interaction = remember { MutableInteractionSource() }
    val haptic = rememberHaptics()
    val pressed by interaction.collectIsPressedAsState()
    val lift by animateDpAsState(
        targetValue = if (pressed) 2.dp else if (compact) 7.dp else 9.dp,
        animationSpec = motion.bounce(0.5f, 700f),
        label = "cardLift",
    )
    // Pinning from the action sheet is otherwise invisible on the card that changed;
    // the pin itself pops so the eye lands on it.
    val pinPop by rememberPopScale(note.pinned)

    val tasks = remember(note.body) { taskProgress(note.body) }

    Column(
        modifier = modifier
            .onGloballyPositioned { coords ->
                // Reported on layout, not per frame: the root keeps this rect in a
                // plain map and only reads it at the instant the card is tapped, so
                // it can grow the editor out of this exact tile.
                onBounds?.invoke(coords.boundsInWindow())
            }
            .pressTilt(
                interactionSource = interaction,
                pressedScale = 0.968f,
                tiltDegrees = 3.4f,
                cornerRadius = corner.dp,
                sheenAlpha = if (dark) 0.10f else 0.20f,
            )
            .shadow(
                elevation = lift,
                shape = shape,
                ambientColor = accent.primary.copy(alpha = 0.22f),
                spotColor = accent.primary.copy(alpha = 0.26f),
            )
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(wash, tint, ColorMath.mix(tint, scheme.surface, 0.35f)),
                    start = Offset.Zero,
                    end = Offset(620f, 900f),
                ),
            )
            .border(
                width = if (selected) 2.dp else 1.dp,
                brush = if (selected) {
                    SolidColor(accent.primary)
                } else {
                    Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = if (dark) 0.13f else 0.62f),
                            Color.White.copy(alpha = if (dark) 0.04f else 0.18f),
                            Color.Transparent,
                        ),
                    )
                },
                shape = shape,
            )
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                onClick = {
                    haptic(Haptic.Tap)
                    onClick()
                },
                onLongClick = {
                    haptic(Haptic.LongPress)
                    onLongClick()
                },
            )
            .padding(horizontal = if (narrow) 12.dp else 14.dp, vertical = 13.dp),
    ) {
        if (tasks != null && tasks.second > 0) {
            val taskFill by animateFloatAsState(
                targetValue = tasks.first.toFloat() / tasks.second.toFloat(),
                animationSpec = motion.bounce(0.55f, 600f),
                label = "taskFill",
            )
            val track = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(CircleShape)
                .background(ink.copy(alpha = 0.09f))
            Box(track) {
                val meter = Modifier
                    .fillMaxWidth(taskFill.coerceIn(0f, 1f))
                    .height(3.dp)
                    .clip(CircleShape)
                    .background(accent.primary.copy(alpha = 0.75f))
                Box(meter)
            }
            Spacer(Modifier.height(10.dp))
        }

        Row(verticalAlignment = Alignment.Top) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = if (settings.serifTitles) Fraunces else MaterialTheme.typography.titleMedium.fontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = when {
                        narrow -> 14.5.sp * settings.fontScale
                        compact -> 16.5.sp * settings.fontScale
                        else -> 20.sp * settings.fontScale
                    },
                    lineHeight = when {
                        narrow -> 19.sp * settings.fontScale
                        compact -> 21.sp * settings.fontScale
                        else -> 26.sp * settings.fontScale
                    },
                ),
                color = ink,
                maxLines = if (compact || narrow) 3 else 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            // A selected card keeps a single unmistakable signal in the title row.
            if (selected) {
                Spacer(Modifier.width(5.dp))
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = stringResource(R.string.note_selected),
                    tint = accent.primary,
                    modifier = Modifier.size(18.dp),
                )
            }
            // A 3-up card has no room for a pin beside the title without breaking
            // "Welcome" mid-word, so the pin moves down to the date line.
            if (note.pinned && !narrow) {
                Spacer(Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Rounded.PushPin,
                    contentDescription = stringResource(R.string.note_pinned),
                    tint = accent.primary,
                    modifier = Modifier
                        .size(13.dp)
                        .graphicsLayer {
                            scaleX = pinPop
                            scaleY = pinPop
                        }
                        .padding(top = 1.dp),
                )
            }
        }

        if (preview.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = preview,
                style = MaterialTheme.typography.bodySmall,
                color = ink.copy(alpha = 0.68f),
                maxLines = when {
                    narrow -> 4
                    compact -> 7
                    else -> 4
                },
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (note.tags.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            val shownTags = if (narrow) 1 else 2
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                note.tags.take(shownTags).forEach { tag ->
                    MiniTag(tag, FolioSchemes.tagColor(tag, dark), dark = dark)
                }
                if (note.tags.size > shownTags) {
                    MiniTag("+${note.tags.size - shownTags}", ink.copy(alpha = 0.5f), dark = dark)
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(accent.primary.copy(alpha = 0.85f)),
            )
            Spacer(Modifier.width(6.dp))
            if (note.pinned && narrow) {
                Icon(
                    imageVector = Icons.Rounded.PushPin,
                    contentDescription = stringResource(R.string.note_pinned),
                    tint = accent.primary,
                    modifier = Modifier
                        .size(10.dp)
                        .graphicsLayer {
                            scaleX = pinPop
                            scaleY = pinPop
                        },
                )
                Spacer(Modifier.width(4.dp))
            }
            Text(
                text = NoteQuery.relativeTime(System.currentTimeMillis(), note.updatedAt),
                style = MaterialTheme.typography.labelSmall,
                color = ink.copy(alpha = 0.5f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                // The date yields width to the counter, so "1/4" can never be
                // squeezed into two lines on a narrow card.
                modifier = Modifier.weight(1f),
            )
            if (tasks != null && tasks.second > 0) {
                Text(
                    text = "${tasks.first}/${tasks.second}",
                    style = MaterialTheme.typography.labelSmall,
                    color = ink.copy(alpha = 0.5f),
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun MiniTag(text: String, color: Color, dark: Boolean) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = if (dark) 0.26f else 0.34f))
            .padding(horizontal = 7.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** checked-to-total for task-list notes, or null when the note has none. */
fun taskProgress(markdown: String): Pair<Int, Int>? {
    if (!markdown.contains("[")) return null
    var done = 0
    var total = 0
    markdown.lineSequence().forEach { raw ->
        val line = raw.trimStart()
        if (line.length >= 5 && (line[0] == '-' || line[0] == '*') && line[1] == ' ' &&
            line[2] == '[' && line[4] == ']'
        ) {
            val mark = line[3]
            if (mark == ' ' || mark == 'x' || mark == 'X') {
                total++
                if (mark != ' ') done++
            }
        }
    }
    return if (total == 0) null else done to total
}
