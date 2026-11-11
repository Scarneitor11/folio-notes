package com.folio.notes.ui.theme

import android.os.Build
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.folio.notes.data.FolioSettings
import com.folio.notes.data.ThemeMode
import com.folio.notes.logic.FolioPalette
import com.folio.notes.logic.FolioSchemes
import com.folio.notes.logic.Palettes
import androidx.compose.foundation.isSystemInDarkTheme

@Immutable
data class FolioMotion(
    val scale: Float,
    /** Set when the OS asks for reduced motion: transitions collapse instead of gliding. */
    val reduced: Boolean = false,
) {
    fun millis(base: Int): Int =
        if (reduced) 1 else (base / scale.coerceIn(0.35f, 3f)).toInt().coerceAtLeast(30)

    fun <T> enter(base: Int = 420): FiniteAnimationSpec<T> =
        tween(durationMillis = millis(base), easing = FastOutSlowInEasing)

    /**
     * Spring for taps, chips and icons; one frame when motion is off.
     *
     * The damping floor is deliberately 1.0: nothing in Folio overshoots its target.
     * The parameter is kept so call sites read as "how soft is the landing", but a
     * request below critical is raised to it - a chip that wobbles past its own size
     * is the difference between a tool and a toy.
     */
    fun <T> bounce(damping: Float = 1f, stiffness: Float = 1400f): FiniteAnimationSpec<T> =
        if (reduced) {
            tween(durationMillis = 1)
        } else {
            spring(dampingRatio = damping.coerceAtLeast(1f), stiffness = stiffness)
        }

    /** Spatial spring for whole-surface slides. Also never overshoots: a page that
     *  bounces shows the background past its own edge. */
    fun slide(damping: Float = 1f, stiffness: Float = 700f): FiniteAnimationSpec<IntOffset> =
        if (reduced) {
            tween(durationMillis = 1)
        } else {
            spring(dampingRatio = damping.coerceAtLeast(1f), stiffness = stiffness)
        }
}

@Immutable
data class FolioDimens(
    val cardCorner: Float,
    val innerCorner: Float,
    val pillCorner: Float,
    val heroCorner: Float,
)

val LocalFolioSettings = staticCompositionLocalOf { FolioSettings() }
val LocalFolioMotion = staticCompositionLocalOf { FolioMotion(1f) }
val LocalFolioDark = staticCompositionLocalOf { false }
val LocalAccent = staticCompositionLocalOf { Palettes.all.first() }
val LocalHighRefresh = compositionLocalOf<(Boolean) -> Unit> { {} }

fun expressiveShape(scale: Float): CornerBasedShape = RoundedCornerShape(
    topStart = (26 * scale).dp,
    topEnd = (10 * scale).dp,
    bottomEnd = (26 * scale).dp,
    bottomStart = (10 * scale).dp,
)

fun folioShapes(cornerScale: Float): Shapes {
    val s = cornerScale.coerceIn(0.3f, 2f)
    return Shapes(
        extraSmall = RoundedCornerShape((6 * s).dp),
        small = RoundedCornerShape((12 * s).dp),
        medium = RoundedCornerShape((18 * s).dp),
        large = RoundedCornerShape((26 * s).dp),
        extraLarge = RoundedCornerShape((34 * s).dp),
    )
}

fun extraLargeShape(settings: FolioSettings): Shape = RoundedCornerShape((34 * settings.cornerScale).dp)

@Composable
private fun animatedColor(target: Color, duration: Int, label: String): Color =
    androidx.compose.animation.animateColorAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = duration, easing = FastOutSlowInEasing),
        label = label,
    ).value

/**
 * `ColorScheme.copy` is internal to Material3, so rebuild the whole scheme through the public
 * factory functions. Every role the app reads is carried over explicitly.
 */
@Suppress("LongParameterList")
private fun schemeWith(
    base: ColorScheme,
    dark: Boolean,
    primary: Color = base.primary,
    onPrimary: Color = base.onPrimary,
    primaryContainer: Color = base.primaryContainer,
    onPrimaryContainer: Color = base.onPrimaryContainer,
    inversePrimary: Color = base.inversePrimary,
    secondary: Color = base.secondary,
    onSecondary: Color = base.onSecondary,
    secondaryContainer: Color = base.secondaryContainer,
    onSecondaryContainer: Color = base.onSecondaryContainer,
    tertiary: Color = base.tertiary,
    onTertiary: Color = base.onTertiary,
    tertiaryContainer: Color = base.tertiaryContainer,
    onTertiaryContainer: Color = base.onTertiaryContainer,
    background: Color = base.background,
    onBackground: Color = base.onBackground,
    surface: Color = base.surface,
    onSurface: Color = base.onSurface,
    surfaceVariant: Color = base.surfaceVariant,
    onSurfaceVariant: Color = base.onSurfaceVariant,
    surfaceTint: Color = base.surfaceTint,
    inverseSurface: Color = base.inverseSurface,
    inverseOnSurface: Color = base.inverseOnSurface,
    error: Color = base.error,
    onError: Color = base.onError,
    errorContainer: Color = base.errorContainer,
    onErrorContainer: Color = base.onErrorContainer,
    outline: Color = base.outline,
    outlineVariant: Color = base.outlineVariant,
    scrim: Color = base.scrim,
    surfaceBright: Color = base.surfaceBright,
    surfaceDim: Color = base.surfaceDim,
    surfaceContainerLowest: Color = base.surfaceContainerLowest,
    surfaceContainerLow: Color = base.surfaceContainerLow,
    surfaceContainer: Color = base.surfaceContainer,
    surfaceContainerHigh: Color = base.surfaceContainerHigh,
    surfaceContainerHighest: Color = base.surfaceContainerHighest,
): ColorScheme {
    return if (dark) {
        androidx.compose.material3.darkColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = primaryContainer,
            onPrimaryContainer = onPrimaryContainer,
            inversePrimary = inversePrimary,
            secondary = secondary,
            onSecondary = onSecondary,
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = onSecondaryContainer,
            tertiary = tertiary,
            onTertiary = onTertiary,
            tertiaryContainer = tertiaryContainer,
            onTertiaryContainer = onTertiaryContainer,
            background = background,
            onBackground = onBackground,
            surface = surface,
            onSurface = onSurface,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = onSurfaceVariant,
            surfaceTint = surfaceTint,
            inverseSurface = inverseSurface,
            inverseOnSurface = inverseOnSurface,
            error = error,
            onError = onError,
            errorContainer = errorContainer,
            onErrorContainer = onErrorContainer,
            outline = outline,
            outlineVariant = outlineVariant,
            scrim = scrim,
            surfaceBright = surfaceBright,
            surfaceDim = surfaceDim,
            surfaceContainerLowest = surfaceContainerLowest,
            surfaceContainerLow = surfaceContainerLow,
            surfaceContainer = surfaceContainer,
            surfaceContainerHigh = surfaceContainerHigh,
            surfaceContainerHighest = surfaceContainerHighest,
        )
    } else {
        androidx.compose.material3.lightColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = primaryContainer,
            onPrimaryContainer = onPrimaryContainer,
            inversePrimary = inversePrimary,
            secondary = secondary,
            onSecondary = onSecondary,
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = onSecondaryContainer,
            tertiary = tertiary,
            onTertiary = onTertiary,
            tertiaryContainer = tertiaryContainer,
            onTertiaryContainer = onTertiaryContainer,
            background = background,
            onBackground = onBackground,
            surface = surface,
            onSurface = onSurface,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = onSurfaceVariant,
            surfaceTint = surfaceTint,
            inverseSurface = inverseSurface,
            inverseOnSurface = inverseOnSurface,
            error = error,
            onError = onError,
            errorContainer = errorContainer,
            onErrorContainer = onErrorContainer,
            outline = outline,
            outlineVariant = outlineVariant,
            scrim = scrim,
            surfaceBright = surfaceBright,
            surfaceDim = surfaceDim,
            surfaceContainerLowest = surfaceContainerLowest,
            surfaceContainerLow = surfaceContainerLow,
            surfaceContainer = surfaceContainer,
            surfaceContainerHigh = surfaceContainerHigh,
            surfaceContainerHighest = surfaceContainerHighest,
        )
    }
}

/** Crossfades every role so switching palette or light/dark glides instead of snapping. */
@Composable
private fun animatedScheme(target: ColorScheme, dark: Boolean, duration: Int): ColorScheme {
    return schemeWith(
        base = target,
        dark = dark,
        primary = animatedColor(target.primary, duration, "primary"),
        onPrimary = animatedColor(target.onPrimary, duration, "onPrimary"),
        primaryContainer = animatedColor(target.primaryContainer, duration, "primaryContainer"),
        onPrimaryContainer = animatedColor(target.onPrimaryContainer, duration, "onPrimaryContainer"),
        inversePrimary = animatedColor(target.inversePrimary, duration, "inversePrimary"),
        secondary = animatedColor(target.secondary, duration, "secondary"),
        onSecondary = animatedColor(target.onSecondary, duration, "onSecondary"),
        secondaryContainer = animatedColor(target.secondaryContainer, duration, "secondaryContainer"),
        onSecondaryContainer = animatedColor(target.onSecondaryContainer, duration, "onSecondaryContainer"),
        tertiary = animatedColor(target.tertiary, duration, "tertiary"),
        onTertiary = animatedColor(target.onTertiary, duration, "onTertiary"),
        tertiaryContainer = animatedColor(target.tertiaryContainer, duration, "tertiaryContainer"),
        onTertiaryContainer = animatedColor(target.onTertiaryContainer, duration, "onTertiaryContainer"),
        background = animatedColor(target.background, duration, "background"),
        onBackground = animatedColor(target.onBackground, duration, "onBackground"),
        surface = animatedColor(target.surface, duration, "surface"),
        onSurface = animatedColor(target.onSurface, duration, "onSurface"),
        surfaceVariant = animatedColor(target.surfaceVariant, duration, "surfaceVariant"),
        onSurfaceVariant = animatedColor(target.onSurfaceVariant, duration, "onSurfaceVariant"),
        surfaceTint = animatedColor(target.surfaceTint, duration, "surfaceTint"),
        inverseSurface = animatedColor(target.inverseSurface, duration, "inverseSurface"),
        inverseOnSurface = animatedColor(target.inverseOnSurface, duration, "inverseOnSurface"),
        error = animatedColor(target.error, duration, "error"),
        onError = animatedColor(target.onError, duration, "onError"),
        errorContainer = animatedColor(target.errorContainer, duration, "errorContainer"),
        onErrorContainer = animatedColor(target.onErrorContainer, duration, "onErrorContainer"),
        outline = animatedColor(target.outline, duration, "outline"),
        outlineVariant = animatedColor(target.outlineVariant, duration, "outlineVariant"),
        scrim = animatedColor(target.scrim, duration, "scrim"),
        surfaceBright = animatedColor(target.surfaceBright, duration, "surfaceBright"),
        surfaceDim = animatedColor(target.surfaceDim, duration, "surfaceDim"),
        surfaceContainerLowest = animatedColor(target.surfaceContainerLowest, duration, "scl"),
        surfaceContainerLow = animatedColor(target.surfaceContainerLow, duration, "sclow"),
        surfaceContainer = animatedColor(target.surfaceContainer, duration, "sc"),
        surfaceContainerHigh = animatedColor(target.surfaceContainerHigh, duration, "sch"),
        surfaceContainerHighest = animatedColor(target.surfaceContainerHighest, duration, "schh"),
    )
}

@Composable
fun FolioTheme(
    settings: FolioSettings,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val dark = when (settings.themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK, ThemeMode.AMOLED -> true
    }
    val amoled = settings.themeMode == ThemeMode.AMOLED
    val context = LocalContext.current
    val palette: FolioPalette = remember(settings.paletteIndex) { Palettes.get(settings.paletteIndex) }

    val target = remember(settings, dark, amoled, context) {
        val useDynamic = settings.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        val base = if (useDynamic) {
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } else {
            FolioSchemes.build(palette, dark, amoled, settings.contrast)
        }
        if (useDynamic) {
            schemeWith(
                base = base,
                dark = dark,
                background = if (dark && amoled) Color.Black else base.background,
                surface = if (dark && amoled) Color.Black else base.surface,
                surfaceContainerLowest = if (dark && amoled) Color.Black else base.surfaceContainerLowest,
                surfaceContainerLow = if (dark && amoled) Color(0xFF080706) else base.surfaceContainerLow,
            )
        } else {
            base
        }
    }

    // Respect the OS "remove animations" accessibility setting ahead of the in-app slider.
    val reduceMotion = rememberReducedMotion()
    val motion = remember(settings.motionScale, reduceMotion) {
        FolioMotion(settings.motionScale, reduceMotion)
    }
    val scheme = animatedScheme(target, dark, motion.millis(420))
    val typography = remember(settings.fontScale, settings.serifTitles) {
        folioTypography(settings.fontScale, settings.serifTitles)
    }
    val shapes = remember(settings.cornerScale) { folioShapes(settings.cornerScale) }

    CompositionLocalProvider(
        LocalFolioSettings provides settings,
        LocalFolioMotion provides motion,
        LocalFolioDark provides dark,
        LocalAccent provides palette,
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = typography,
            shapes = shapes,
            content = content,
        )
    }
}

/**
 * Slowly breathing value in 0..1, used by the ambient background.
 *
 * Reading this directly in composition re-runs the caller on every display frame for
 * the life of the screen. Prefer [rememberBreathingState] and read `.value` inside a
 * draw or layer block, where only the layer is invalidated.
 */
@Composable
fun rememberBreathing(key: Any? = Unit, periodMillis: Int = 22000): Float =
    rememberBreathingState(key, periodMillis).value

/**
 * [rememberBreathing] as observable state. The value only changes while the frame
 * clock runs, so deferring the read to `graphicsLayer` or `Canvas` keeps an ambient
 * animation off the recomposition path entirely.
 */
@Composable
fun rememberBreathingState(key: Any? = Unit, periodMillis: Int = 22000): State<Float> {
    // Hold a static midpoint instead of looping something the OS asked us to stop.
    if (LocalFolioMotion.current.reduced) {
        return remember { mutableFloatStateOf(0.5f) }
    }
    val transition = rememberInfiniteTransition(label = "breathing-$key")
    return transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(periodMillis, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "breath",
    )
}

@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    val scale = remember(context) {
        runCatching {
            android.provider.Settings.Global.getFloat(context.contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        }.getOrDefault(1f)
    }
    return scale == 0f
}

/** Density-aware pixel helper. */
@Composable
fun dpToPx(value: Float): Float = with(LocalDensity.current) { value.dp.toPx() }

@Composable
fun animateCorner(progress: Float, base: Float = 26f): Shape =
    RoundedCornerShape((base * progress).dp)
