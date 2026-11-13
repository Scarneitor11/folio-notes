package com.folio.notes.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.folio.notes.logic.ColorMath
import com.folio.notes.logic.FolioPalette
import com.folio.notes.ui.theme.Fraunces
import com.folio.notes.ui.theme.LocalFolioDark
import com.folio.notes.ui.theme.LocalFolioMotion
import com.folio.notes.ui.theme.LocalFolioSettings
import com.folio.notes.ui.theme.rememberBreathingState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Ambient backdrop: three slow radial blooms plus a soft vignette. It is drawn
 * rather than asset based so it recolours with the palette.
 */
@Composable
fun AuroraBackground(
    modifier: Modifier = Modifier,
    accent: FolioPalette,
    blur: Boolean = true,
) {
    val dark = LocalFolioDark.current
    val scheme = MaterialTheme.colorScheme
    // Ambient drift settles to a still pose when the OS asks for reduced motion.
    // The states stay out of composition: only the Canvas draw reads them.
    val driftA = rememberBreathingState(key = "drift", periodMillis = 26000)
    val driftB = rememberBreathingState(key = "driftB", periodMillis = 34000)
    val driftC = rememberBreathingState(key = "driftC", periodMillis = 19000)

    // These blooms travel for half a minute, so stepping the pose 128 times per
    // sweep is below the eye's threshold. It matters because the full-screen blur
    // would otherwise re-render at the display's refresh rate, forever.
    val drift = remember(driftA) { derivedStateOf { quantizePose(driftA.value) } }
    val driftBQ = remember(driftB) { derivedStateOf { quantizePose(driftB.value) } }
    val driftCQ = remember(driftC) { derivedStateOf { quantizePose(driftC.value) } }

    val strength = if (dark) 0.34f else 0.42f
    // The wash only changes with the theme, so it does not belong in the frame loop.
    val vignette = remember(scheme.background) {
        Brush.verticalGradient(
            0f to Color.Transparent,
            0.55f to scheme.background.copy(alpha = 0.20f),
            1f to scheme.background.copy(alpha = 0.72f),
        )
    }

    Box(modifier.background(scheme.background)) {
        androidx.compose.foundation.Canvas(
            modifier = Modifier
                .matchParentSize()
                .then(
                    if (blur) Modifier.blur(72.dp) else Modifier,
                ),
        ) {
            val pA = drift.value
            val pB = driftBQ.value
            val pC = driftCQ.value
            bloom(
                center = Offset(size.width * (0.08f + 0.14f * pA), size.height * (0.02f + 0.06f * pB)),
                radius = size.minDimension * 0.92f,
                color = accent.primary.copy(alpha = strength),
            )
            bloom(
                center = Offset(size.width * (1.02f - 0.16f * pB), size.height * (0.22f + 0.12f * pA)),
                radius = size.minDimension * 0.80f,
                color = accent.tertiary.copy(alpha = strength * 0.82f),
            )
            bloom(
                center = Offset(size.width * (0.42f + 0.18f * pC), size.height * (1.02f - 0.10f * pC)),
                radius = size.minDimension * 0.95f,
                color = accent.secondary.copy(alpha = strength * 0.70f),
            )
        }
        // A faint warm wash keeps the middle readable instead of a flat gradient.
        Box(
            Modifier
                .matchParentSize()
                .background(vignette),
        )
    }
}

/** Steps 0..1 into 128 buckets so a glacial drift only invalidates the draw ~5×/s. */
private fun quantizePose(value: Float): Float =
    (value * 128f).roundToInt() / 128f

private fun DrawScope.bloom(center: Offset, radius: Float, color: Color) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color, color.copy(alpha = color.alpha * 0.35f), Color.Transparent),
            center = center,
            radius = radius,
        ),
        radius = radius,
        center = center,
    )
}

/** Springy press feedback used by every tappable surface. */
@Composable
fun Modifier.pressScale(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = 0.955f,
): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val motion = LocalFolioMotion.current
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = motion.bounce(damping = 0.28f, stiffness = 1700f),
        label = "pressScale",
    )
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(28.dp),
    tint: Color = Color.Unspecified,
    accent: Color = MaterialTheme.colorScheme.primary,
    borderAlpha: Float = 0.35f,
    elevation: Dp = 10.dp,
    content: @Composable () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val base = if (tint == Color.Unspecified) scheme.surface else tint
    val wash = if (tint == Color.Unspecified) base else accent
    Box(
        modifier = modifier
            .shadow(
                elevation = elevation,
                shape = shape,
                ambientColor = accent.copy(alpha = 0.36f),
                spotColor = accent.copy(alpha = 0.30f),
            )
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        base.copy(alpha = 0.96f),
                        wash.copy(alpha = if (tint == Color.Unspecified) 0.86f else 0.92f),
                    ),
                    start = Offset.Zero,
                    end = Offset(900f, 1400f),
                ),
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = if (LocalFolioDark.current) 0.14f else 0.55f),
                        accent.copy(alpha = borderAlpha),
                        Color.Transparent,
                    ),
                ),
                shape = shape,
            ),
    ) {
        content()
    }
}

/** Staggered pop for list items; only the first screenful animates. */
@Composable
fun Reveal(
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val motion = LocalFolioMotion.current
    // Only the first screenful gets an entrance. Lazy lists create items as they
    // scroll into view, and animating every one of those made a long library feel
    // like it was continuously loading instead of simply moving.
    val animateIn = index in 0 until 14 && !motion.reduced
    var visible by remember(animateIn) { mutableStateOf(!animateIn) }
    LaunchedEffect(animateIn, index) {
        if (animateIn) {
            delay((index.coerceAtLeast(0) * 22L * (1f / motion.scale.coerceIn(0.5f, 2f))).toLong())
            visible = true
        }
    }
    val progress by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = motion.bounce(damping = 0.4f, stiffness = 520f),
        label = "reveal",
    )
    val alpha = progress.coerceIn(0f, 1f)
    Box(
        modifier.graphicsLayer {
            this.alpha = alpha
            scaleX = 0.86f + 0.14f * progress
            scaleY = 0.86f + 0.14f * progress
            translationY = (1f - progress) * 34f
            rotationZ = (1f - progress) * -1.4f
        },
    ) {
        content()
    }
}

@Composable
fun FolioMark(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    androidx.compose.foundation.Canvas(modifier) {
        val w = size.width
        val h = size.height
        val radius = w * 0.30f
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.16f, h * 0.10f),
            size = androidx.compose.ui.geometry.Size(w * 0.68f, h * 0.80f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.085f),
        )
        val line = w * 0.085f
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.33f, h * 0.34f),
            size = androidx.compose.ui.geometry.Size(w * 0.34f, line * 0.85f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(line, line),
        )
        drawRoundRect(
            color = color.copy(alpha = 0.65f),
            topLeft = Offset(w * 0.33f, h * 0.50f),
            size = androidx.compose.ui.geometry.Size(w * 0.34f, line * 0.85f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(line, line),
        )
        drawRoundRect(
            color = color.copy(alpha = 0.40f),
            topLeft = Offset(w * 0.33f, h * 0.66f),
            size = androidx.compose.ui.geometry.Size(w * 0.20f, line * 0.85f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(line, line),
        )
    }
}

@Composable
fun MiniIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    container: Color = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
    size: Dp = 48.dp,
    enabled: Boolean = true,
) {
    val interaction = remember { MutableInteractionSource() }
    val motion = LocalFolioMotion.current
    val pressed by interaction.collectIsPressedAsState()
    val iconScale by animateFloatAsState(
        targetValue = if (pressed) 0.78f else 1f,
        animationSpec = motion.bounce(0.28f, 1500f),
        label = "miniIcon",
    )
    val iconSpin by animateFloatAsState(
        targetValue = if (pressed) -12f else 0f,
        animationSpec = motion.bounce(0.3f, 1200f),
        label = "miniSpin",
    )
    Box(
        modifier = modifier
            .size(size)
            .pressTilt(
                interactionSource = interaction,
                pressedScale = 0.90f,
                tiltDegrees = 11f,
                cornerRadius = size * 0.5f,
                sheenAlpha = 0.26f,
            )
            .clip(CircleShape)
            .background(container)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), CircleShape)
            .clickableNoRipple(interaction, enabled, onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint.copy(alpha = if (enabled) 1f else 0.35f),
            modifier = Modifier
                .size(size * 0.47f)
                .graphicsLayer {
                    scaleX = iconScale
                    scaleY = iconScale
                    rotationZ = iconSpin
                },
        )
    }
}

@Composable
fun TagChip(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(50)
    val interaction = remember { MutableInteractionSource() }
    val motion = LocalFolioMotion.current
    val fillAlpha = if (selected) 0.95f else 0.30f
    val fill by animateColorAsState(
        targetValue = color.copy(alpha = fillAlpha),
        animationSpec = spring(dampingRatio = 1f, stiffness = 900f),
        label = "chipFill",
    )
    // The label has to be chosen from the colour the eye actually sees, which is
    // the tint composited over the page, not the raw accent. A near-black primary
    // at 30% over paper is mid grey, and mid grey defeats a fixed onSurfaceVariant.
    val ink = ColorMath.inkOn(
        ColorMath.mix(MaterialTheme.colorScheme.background, color, fillAlpha),
    )
    val bump by animateFloatAsState(
        targetValue = if (selected) 1.06f else 1f,
        animationSpec = motion.bounce(0.35f, 1000f),
        label = "chipBump",
    )
    // Selection is a state, so the pop has to come from the transition, not the target.
    val selectPop by rememberPopScale(selected, from = 0.88f)
    Row(
        modifier = modifier
            .graphicsLayer {
                scaleX = bump * selectPop
                scaleY = bump * selectPop
            }
            .clip(shape)
            .background(fill)
            .border(
                1.dp,
                color.copy(alpha = if (selected) 1f else 0.45f),
                shape,
            )
            .then(
                if (onClick != null) {
                    Modifier
                        .pressTilt(
                            interactionSource = interaction,
                            pressedScale = 0.92f,
                            tiltDegrees = 8f,
                            cornerRadius = 18.dp,
                            sheenAlpha = 0.24f,
                        )
                        .clickableNoRipple(interaction, true, onClick)
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 9.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            // Selected chips are filled with an accent that may itself be near
            // black (dynamic colour from a dark wallpaper), so the label has to
            // be picked from the fill instead of assumed to be dark ink.
            color = ink,
            maxLines = 1,
        )
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        letterSpacing = androidx.compose.ui.unit.TextUnit.Unspecified,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
        modifier = modifier,
    )
}

/* ------------------------------------------------------------------------- */
/* Dialog shell                                                              */
/* ------------------------------------------------------------------------- */

/**
 * Shared modal shell. The platform dim is switched off and replaced with a
 * scrim we can animate; the card springs in and plays an accelerated exit
 * before the caller removes it. Every dialog in the app uses this so the
 * presentation matches the rest of the motion language.
 */
@Composable
fun FolioDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val motion = LocalFolioMotion.current
    val settings = LocalFolioSettings.current
    val dark = LocalFolioDark.current
    val scheme = MaterialTheme.colorScheme
    OverlayBlurEffect(true)
    val scope = rememberCoroutineScope()

    var shown by remember { mutableStateOf(false) }
    var closing by remember { mutableStateOf(false) }

    // Kill the platform's dim and window fade so only our own motion is visible.
    val provider = LocalView.current.parent as? DialogWindowProvider
    SideEffect {
        provider?.window?.apply {
            setDimAmount(0f)
            setWindowAnimations(0)
        }
    }

    LaunchedEffect(Unit) { shown = true }

    val close: () -> Unit = {
        if (!closing) {
            closing = true
            if (motion.reduced) {
                onDismissRequest()
            } else {
                scope.launch {
                    delay(110)
                    onDismissRequest()
                }
            }
        }
    }

    val progress by animateFloatAsState(
        targetValue = if (shown && !closing) 1f else 0f,
        animationSpec = if (closing || !shown) {
            tween(durationMillis = if (motion.reduced) 1 else 110)
        } else {
            motion.bounce(damping = 0.46f, stiffness = 720f)
        },
        label = "dialogCard",
    )
    val scrim by animateFloatAsState(
        targetValue = if (shown && !closing) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (motion.reduced) 1 else if (closing) 110 else 170,
        ),
        label = "dialogScrim",
    )

    Dialog(
        onDismissRequest = close,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .pointerInput(Unit) { detectTapGestures { close() } }
                .background(
                    scheme.scrim.copy(
                        alpha = (if (dark) 0.68f else 0.46f) * scrim.coerceIn(0f, 1f),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(horizontal = 26.dp)
                    .graphicsLayer {
                        val p = progress
                        alpha = p.coerceIn(0f, 1f)
                        scaleX = 0.86f + 0.14f * p
                        scaleY = 0.86f + 0.14f * p
                        translationY = (1f - p) * 46f
                    }
                    // Swallow taps on the card so they do not reach the scrim.
                    .pointerInput(Unit) { detectTapGestures { } },
            ) {
                GlassPanel(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape((30 * settings.cornerScale).dp),
                    elevation = 24.dp,
                    borderAlpha = 0.4f,
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 22.dp, vertical = 20.dp),
                        content = content,
                    )
                }
            }
        }
    }
}

@Composable
fun FolioDialogTitle(text: String, modifier: Modifier = Modifier) {
    val settings = LocalFolioSettings.current
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge.copy(
            fontFamily = if (settings.serifTitles) Fraunces else null,
            fontWeight = FontWeight.SemiBold,
        ),
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier,
    )
}

@Composable
fun FolioDialogBody(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(top = 8.dp),
    )
}

@Composable
fun FolioDialogField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        placeholder = if (placeholder != null) {
            { Text(placeholder, style = MaterialTheme.typography.bodyMedium) }
        } else {
            null
        },
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp),
    )
}

@Composable
fun FolioDialogActions(
    confirmLabel: String,
    onConfirm: () -> Unit,
    dismissLabel: String,
    onDismiss: () -> Unit,
    destructive: Boolean = false,
    confirmEnabled: Boolean = true,
) {
    val scheme = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val dismissInteraction = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(
            onClick = onDismiss,
            shape = RoundedCornerShape(50),
            interactionSource = dismissInteraction,
            modifier = Modifier.pressTilt(
                interactionSource = dismissInteraction,
                pressedScale = 0.95f,
                tiltDegrees = 6f,
                cornerRadius = 22.dp,
                sheenAlpha = 0.14f,
            ),
        ) {
            Text(
                text = dismissLabel,
                color = scheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
            )
        }
        Button(
            onClick = onConfirm,
            enabled = confirmEnabled,
            interactionSource = interaction,
            shape = RoundedCornerShape(50),
            modifier = Modifier.pressTilt(
                interactionSource = interaction,
                pressedScale = 0.94f,
                tiltDegrees = 6f,
                cornerRadius = 22.dp,
                sheenAlpha = 0.28f,
            ),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (destructive) scheme.errorContainer else scheme.primary,
                contentColor = if (destructive) scheme.onErrorContainer else scheme.onPrimary,
                disabledContainerColor = scheme.surfaceVariant.copy(alpha = 0.6f),
                disabledContentColor = scheme.onSurfaceVariant.copy(alpha = 0.6f),
            ),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
        ) {
            Text(confirmLabel, fontWeight = FontWeight.SemiBold)
        }
    }
}
