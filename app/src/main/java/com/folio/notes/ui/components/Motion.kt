package com.folio.notes.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.folio.notes.ui.theme.FolioMotion
import com.folio.notes.ui.theme.LocalFolioMotion
import kotlinx.coroutines.delay

/* ------------------------------------------------------------------------- */
/* Specs                                                                     */
/* ------------------------------------------------------------------------- */

/**
 * The four weights of Folio's motion language. Nothing here overshoots: Folio moves
 * like a page being turned, never like a rubber band. Damping is 1.0 throughout, so a
 * surface travels to its target and stops there — the spring is used only for its
 * ability to start at the finger's speed and to be interrupted cleanly mid-flight.
 * A long travel gets a curve instead, because a 40 % screen pan on a spring reads as
 * a drift rather than a scroll. [reduced] collapses every one of them to a single
 * frame so the OS accessibility setting still wins.
 *
 * - [snap]   lands immediately: colours, alphas, press response.
 * - [pop]    the working weight: chips, icons, small surfaces.
 * - [settle] calm and quick: entrances, reveals, header rises.
 * - [glide]  screens and surfaces that cross a long distance.
 */
fun <T> FolioMotion.snap(): FiniteAnimationSpec<T> =
    if (reduced) tween(durationMillis = 1) else spring(dampingRatio = 1f, stiffness = 2600f)

fun <T> FolioMotion.pop(): FiniteAnimationSpec<T> =
    if (reduced) tween(durationMillis = 1) else spring(dampingRatio = 1f, stiffness = 1500f)

fun <T> FolioMotion.settle(): FiniteAnimationSpec<T> =
    if (reduced) tween(durationMillis = 1) else spring(dampingRatio = 1f, stiffness = 820f)

fun <T> FolioMotion.glide(): FiniteAnimationSpec<T> =
    if (reduced) {
        tween(durationMillis = 1)
    } else {
        tween(durationMillis = millis(380), easing = FastOutSlowInEasing)
    }

/**
 * The travel curve for a screen pan. Deliberately a fixed curve, not a spring: a pan
 * that overshoots shows background at the edge it overshoots past, and a pan has to
 * land exactly on the frame or the screen looks misaligned afterwards.
 */
fun FolioMotion.pan(millis: Int = 340): FiniteAnimationSpec<IntOffset> =
    if (reduced) tween(durationMillis = 1) else tween(durationMillis = this.millis(millis), easing = FastOutSlowInEasing)

/** Same curve for a fade that has to arrive with the pan it belongs to. */
fun FolioMotion.fade(millis: Int = 200): FiniteAnimationSpec<Float> =
    if (reduced) tween(durationMillis = 1) else tween(durationMillis = this.millis(millis), easing = FastOutSlowInEasing)

/* ------------------------------------------------------------------------- */
/* Press                                                                     */
/* ------------------------------------------------------------------------- */

/**
 * Three-dimensional press feedback. A tap tips the surface toward the finger and
 * lays a soft pool of light under it, so a card behaves like a physical tile rather
 * than a rectangle that shrinks.
 *
 * The press position comes from the interaction source, which means this adds no
 * pointer handling of its own and cannot steal gestures from `clickable`. Every
 * animated value is read inside `graphicsLayer`/the draw block, so a press
 * invalidates the layer, never composition.
 */
@Composable
fun Modifier.pressTilt(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = 0.965f,
    tiltDegrees: Float = 4.5f,
    cornerRadius: Dp = 24.dp,
    sheen: Color = Color.White,
    sheenAlpha: Float = 0.13f,
): Modifier {
    val motion = LocalFolioMotion.current
    var width by remember { mutableFloatStateOf(1f) }
    var height by remember { mutableFloatStateOf(1f) }
    var pressX by remember { mutableFloatStateOf(0.5f) }
    var pressY by remember { mutableFloatStateOf(0.5f) }
    var down by remember { mutableStateOf(false) }

    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { event ->
            when (event) {
                is PressInteraction.Press -> {
                    if (width > 1f && height > 1f) {
                        pressX = (event.pressPosition.x / width).coerceIn(0f, 1f)
                        pressY = (event.pressPosition.y / height).coerceIn(0f, 1f)
                    }
                    down = true
                }

                is PressInteraction.Release -> down = false
                is PressInteraction.Cancel -> down = false
                else -> Unit
            }
        }
    }

    val active = down && !motion.reduced
    val scale by animateFloatAsState(
        targetValue = if (down && !motion.reduced) pressedScale else 1f,
        animationSpec = motion.snap(),
        label = "tiltScale",
    )
    val rotationX by animateFloatAsState(
        targetValue = if (active) (pressY - 0.5f) * tiltDegrees * 1.6f else 0f,
        animationSpec = motion.pop(),
        label = "tiltX",
    )
    val rotationY by animateFloatAsState(
        targetValue = if (active) -(pressX - 0.5f) * tiltDegrees * 1.6f else 0f,
        animationSpec = motion.pop(),
        label = "tiltY",
    )
    val glow by animateFloatAsState(
        targetValue = if (active) 1f else 0f,
        animationSpec = motion.snap(),
        label = "tiltGlow",
    )

    return this
        .onSizeChanged {
            width = it.width.toFloat().coerceAtLeast(1f)
            height = it.height.toFloat().coerceAtLeast(1f)
        }
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            this.rotationX = rotationX
            this.rotationY = rotationY
            cameraDistance = 14f * density
        }
        .drawWithContent {
            drawContent()
            if (glow > 0.01f && sheenAlpha > 0f) {
                val center = Offset(pressX * size.width, pressY * size.height)
                val radius = size.maxDimension * 0.78f
                drawRoundRect(
                    brush = Brush.radialGradient(
                        colors = listOf(sheen.copy(alpha = sheenAlpha * glow), Color.Transparent),
                        center = center,
                        radius = radius,
                    ),
                    cornerRadius = CornerRadius(cornerRadius.toPx()),
                )
            }
        }
}

/* ------------------------------------------------------------------------- */
/* Entrances                                                                 */
/* ------------------------------------------------------------------------- */

/**
 * A short rise-and-fade for static chrome (headers, labels, toolbars). [index]
 * staggers siblings so a column assembles top-down in about a fifth of a second.
 * Unlike [Reveal] it does not scale, which keeps type crisp while it moves.
 */
@Composable
fun Rise(
    index: Int,
    modifier: Modifier = Modifier,
    stepMillis: Int = 42,
    distance: Float = 15f,
    content: @Composable () -> Unit,
) {
    val motion = LocalFolioMotion.current
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!motion.reduced) delay((index.coerceIn(0, 9) * stepMillis).toLong())
        shown = true
    }
    val progress by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = motion.settle(),
        label = "rise",
    )
    Box(
        modifier.graphicsLayer {
            alpha = progress.coerceIn(0f, 1f)
            translationY = (1f - progress) * distance
        },
    ) {
        content()
    }
}

/**
 * One-shot specular sweep across a surface as it settles. Cheap enough for cards:
 * it only draws for a bit over half a second, then the modifier costs one branch.
 * Reduced motion skips the sweep entirely.
 */
@Composable
fun Modifier.entranceSheen(
    key: Any? = Unit,
    delayMillis: Int = 0,
    alpha: Float = 0.16f,
): Modifier {
    val motion = LocalFolioMotion.current
    val progress = remember { Animatable(1f) }
    LaunchedEffect(key, motion.reduced) {
        if (motion.reduced) {
            progress.snapTo(1f)
            return@LaunchedEffect
        }
        progress.snapTo(0f)
        delay(delayMillis.toLong())
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 620, easing = LinearEasing),
        )
    }
    return drawWithContent {
        drawContent()
        val p = progress.value
        if (p > 0f && p < 1f) {
            val band = size.width * 0.42f
            val x = -band + p * (size.width + 2f * band)
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = alpha),
                        Color.Transparent,
                    ),
                    start = Offset(x - band, 0f),
                    end = Offset(x + band, size.height),
                ),
            )
        }
    }
}

/**
 * Value swap that rolls vertically, like a split-flap display. Used for titles and
 * counters so a state change reads as a physical object turning over rather than a
 * string being replaced.
 */
@Composable
fun <T> RollingContent(
    target: T,
    modifier: Modifier = Modifier,
    direction: (initial: T, target: T) -> Int = { _, _ -> 1 },
    content: @Composable (T) -> Unit,
) {
    val motion = LocalFolioMotion.current
    AnimatedContent(
        targetState = target,
        transitionSpec = {
            val dir = direction(initialState, targetState)
            (slideInVertically(motion.pan()) { height -> dir * height / 2 } +
                fadeIn(tween(motion.millis(170)))) togetherWith
                (slideOutVertically(tween(motion.millis(120))) { height -> -dir * height / 3 } +
                    fadeOut(tween(motion.millis(110))))
        },
        label = "rolling",
        modifier = modifier,
        content = { content(it) },
    )
}

/**
 * A short arrival emphasised when [trigger] changes, for things that have no room
 * to roll: a pin, a badge, a count. It rises from [from] to its own size and stops
 * - no overshoot, because a number that overshoots is a number you have to wait to
 * read. Read the state inside `graphicsLayer` so it never re-runs composition.
 */
@Composable
fun rememberPopScale(trigger: Any?, from: Float = 0.86f): State<Float> {
    val motion = LocalFolioMotion.current
    val anim = remember { Animatable(1f) }
    LaunchedEffect(trigger) {
        if (motion.reduced) {
            anim.snapTo(1f)
            return@LaunchedEffect
        }
        anim.snapTo(from)
        anim.animateTo(targetValue = 1f, animationSpec = motion.settle())
    }
    return anim.asState()
}

/**
 * Cubic-out: most of the growth happens early, so the reveal lands like a page
 * being pulled open rather than creeping open until the last frame.
 */
internal fun expandEase(progress: Float): Float {
    val p = progress.coerceIn(0f, 1f)
    val remaining = 1f - p
    return 1f - remaining * remaining * remaining
}

/**
 * The window rect the surface occupies at [eased] progress. At 0 it is exactly the
 * tile; at 1 it is the whole window. Every edge moves monotonically, so the reveal
 * never jumps backwards.
 */
internal fun expandBounds(origin: Rect, width: Float, height: Float, eased: Float): Rect {
    val e = eased.coerceIn(0f, 1f)
    val remaining = 1f - e
    return Rect(
        left = origin.left * remaining,
        top = origin.top * remaining,
        right = width + (origin.right - width) * remaining,
        bottom = height + (origin.bottom - height) * remaining,
    )
}

/** Corner radius of the growing surface: the tile's roundness fades out as it fills. */
internal fun expandRadius(maxRadiusPx: Float, eased: Float): Float =
    (1f - eased.coerceIn(0f, 1f)) * maxRadiusPx

/**
 * Reveals a full-window surface out of [origin] - the rect of the card that opened it.
 *
 * The surface is laid out at its final size from the first frame and only its clip
 * grows: the tile's rectangle widens into the window while its corner radius fades
 * to zero. Nothing is scaled, so the editor's text is razor sharp on frame one and
 * the GPU never resamples a 1080x2400 layer - which is exactly the cost that made
 * the old full-screen scale-in lose two thirds of its frames.
 *
 * [progress] is read inside the draw block, so a 380 ms reveal costs one clip update
 * per frame and zero recompositions - the same rule the ambient loops follow.
 *
 * A null [origin] (a note opened from the action sheet, a fresh note) leaves the
 * modifier inert; the caller's own pan covers that case.
 */
fun Modifier.expandFrom(
    progress: State<Float>,
    origin: Rect?,
    window: IntSize,
): Modifier {
    if (origin == null || window.width == 0 || window.height == 0) return this
    return drawWithContent {
        val e = expandEase(progress.value)
        val bounds = expandBounds(origin, size.width, size.height, e)
        val radius = expandRadius(28.dp.toPx(), e)
        val reveal = Path().apply {
            addRoundRect(RoundRect(bounds, CornerRadius(radius, radius)))
        }
        clipPath(reveal) { this@drawWithContent.drawContent() }
    }
}

/* ------------------------------------------------------------------------- */
/* Stagger helpers                                                           */
/* ------------------------------------------------------------------------- */

/**
 * Delay for the [index]-th item of a cascade, in milliseconds, scaled by the
 * user's motion preference. Capped so a long list never waits more than ~0.3 s
 * before its first screenful is visible.
 */
fun FolioMotion.stagger(index: Int, stepMillis: Int = 26): Long =
    if (reduced) 0L else (index.coerceIn(0, 11) * stepMillis / scale.coerceIn(0.5f, 2f)).toLong()

/** Compose's default `SizeTransform` would clip the rolling text; keep it disabled. */
@Suppress("unused")
private val noSizeTransform = SizeTransform(clip = false)
