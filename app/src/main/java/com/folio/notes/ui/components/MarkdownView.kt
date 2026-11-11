package com.folio.notes.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.folio.notes.R
import com.folio.notes.logic.Block
import com.folio.notes.logic.MarkStyle
import com.folio.notes.logic.MarkdownLite
import com.folio.notes.logic.Span
import com.folio.notes.ui.theme.Fraunces
import com.folio.notes.ui.theme.LocalAccent
import com.folio.notes.ui.theme.LocalFolioMotion
import com.folio.notes.ui.theme.LocalFolioSettings

/**
 * Renders the markdown-lite dialect as real typography rather than a monospace
 * dump. Every block styles itself from the active theme, so a palette switch
 * reads through the whole document.
 */
@Composable
fun MarkdownBody(
    markdown: String,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    onToggleTask: ((Int) -> Unit)? = null,
) {
    val settings = LocalFolioSettings.current
    val blocks = remember(markdown) { MarkdownLite.parse(markdown) }
    val codeBg = MaterialTheme.colorScheme.surfaceContainerHigh
    val serif = settings.serifTitles

    Column(modifier = modifier.fillMaxWidth()) {
        blocks.forEachIndexed { index, block ->
            when (block) {
                is Block.Heading -> {
                    if (index > 0) Spacer(Modifier.height(if (block.level <= 2) 18.dp else 12.dp))
                    Text(
                        text = inline(block.spans, textColor, codeBg),
                        style = MaterialTheme.typography.run {
                            when (block.level) {
                                1 -> displaySmall
                                2 -> headlineMedium
                                3 -> headlineSmall
                                else -> titleLarge
                            }
                        }.copy(
                            fontFamily = when {
                                block.level <= 2 && serif -> Fraunces
                                block.level <= 2 -> MaterialTheme.typography.displaySmall.fontFamily
                                else -> null
                            },
                            color = textColor,
                        ),
                    )
                    Spacer(Modifier.height(6.dp))
                }

                is Block.Paragraph -> {
                    Text(
                        text = inline(block.spans, textColor, codeBg),
                        style = MaterialTheme.typography.bodyLarge.copy(color = textColor, lineHeight = 26.sp),
                    )
                    Spacer(Modifier.height(10.dp))
                }

                is Block.Bullet -> BulletRow(
                    block = block,
                    textColor = textColor,
                    codeBg = codeBg,
                    onToggleTask = onToggleTask,
                )

                is Block.Quote -> {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                    ) {
                        Box(
                            Modifier
                                .width(3.dp)
                                .height(24.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(LocalAccent.current.primary.copy(alpha = 0.6f)),
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = inline(block.spans, textColor, codeBg),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontStyle = FontStyle.Italic,
                                color = textColor.copy(alpha = 0.86f),
                                lineHeight = 25.sp,
                            ),
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                }

                is Block.Code -> {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(codeBg)
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                RoundedCornerShape(16.dp),
                            )
                            .padding(14.dp),
                    ) {
                        Text(
                            text = block.code.ifBlank { " " },
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                fontSize = 13.sp,
                                lineHeight = 20.sp,
                                color = textColor.copy(alpha = 0.9f),
                            ),
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                }

                Block.Rule -> {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f)),
                    )
                }
            }
        }
    }
}

@Composable
private fun BulletRow(
    block: Block.Bullet,
    textColor: Color,
    codeBg: Color,
    onToggleTask: ((Int) -> Unit)?,
) {
    val accent = LocalAccent.current.primary
    val motion = LocalFolioMotion.current
    val boxInteraction = remember { MutableInteractionSource() }
    val pressed by boxInteraction.collectIsPressedAsState()
    val doneAlpha by animateFloatAsState(
        targetValue = if (block.checked == true) 0.55f else 1f,
        animationSpec = motion.bounce(0.6f, 600f),
        label = "doneAlpha",
    )
    val boxScale by animateFloatAsState(
        targetValue = if (pressed) 0.78f else 1f,
        animationSpec = motion.bounce(0.26f, 1600f),
        label = "checkScale",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (block.indent * 14).dp, bottom = 7.dp),
        verticalAlignment = Alignment.Top,
    ) {
        when (block.checked) {
            null -> Text(
                text = "•",
                style = MaterialTheme.typography.bodyLarge,
                color = accent.copy(alpha = 0.9f),
                modifier = Modifier.width(18.dp),
            )

            else -> {
                val done = block.checked
                Box(
                    modifier = Modifier
                        .padding(top = 3.dp)
                        .size(18.dp)
                        .graphicsLayer {
                            scaleX = boxScale
                            scaleY = boxScale
                        }
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (done) accent else Color.Transparent)
                        .border(
                            1.5.dp,
                            if (done) accent else MaterialTheme.colorScheme.outline,
                            RoundedCornerShape(6.dp),
                        )
                        .then(
                            if (onToggleTask != null) {
                                Modifier.pressScale(boxInteraction, 0.8f)
                                    .clickableNoRipple(boxInteraction, true) { onToggleTask(block.line) }
                            } else {
                                Modifier
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (done) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = stringResource(R.string.markdown_completed),
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(12.dp),
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
            }
        }
        Text(
            text = inline(block.spans, textColor, codeBg),
            style = MaterialTheme.typography.bodyLarge.copy(
                color = if (block.checked == true) textColor.copy(alpha = doneAlpha) else textColor,
                textDecoration = if (block.checked == true) TextDecoration.LineThrough else null,
                lineHeight = 24.sp,
            ),
        )
    }
}

/** Turns inline spans into one annotated run, so wrapping stays natural. */
fun inline(spans: List<Span>, textColor: Color, codeBg: Color): AnnotatedString = buildAnnotatedString {
    spans.forEach { span ->
        val start = length
        append(span.text)
        val style = when (span.style) {
            MarkStyle.NORMAL -> null
            MarkStyle.BOLD -> SpanStyle(fontWeight = FontWeight.SemiBold)
            MarkStyle.ITALIC -> SpanStyle(fontStyle = FontStyle.Italic)
            MarkStyle.BOLD_ITALIC -> SpanStyle(
                fontWeight = FontWeight.SemiBold,
                fontStyle = FontStyle.Italic,
            )
            MarkStyle.CODE -> SpanStyle(
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                background = codeBg,
                fontSize = 14.sp,
            )
            MarkStyle.STRIKE -> SpanStyle(textDecoration = TextDecoration.LineThrough)
        }
        if (style != null) addStyle(style, start, length)
    }
}

/** Compact inline preview used on cards when a rich render would be too heavy. */
@Composable
fun InlinePreview(markdown: String, maxLines: Int, modifier: Modifier = Modifier) {
    val textColor = MaterialTheme.colorScheme.onSurface
    val spans = remember(markdown) {
        MarkdownLite.plainText(markdown).take(400)
    }
    Text(
        text = spans,
        style = MaterialTheme.typography.bodyMedium.copy(color = textColor.copy(alpha = 0.7f)),
        maxLines = maxLines,
        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

/** A quiet label row used above editors and sheets. */
@Composable
fun MetaRow(vararg items: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEach { item ->
            Text(
                text = item,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
            )
        }
    }
}
