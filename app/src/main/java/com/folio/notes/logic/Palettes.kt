package com.folio.notes.logic

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.math.abs

/** An accent identity. Everything else is derived so the palettes stay coherent. */
data class FolioPalette(
    val name: String,
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
)

object Palettes {
    val all = listOf(
        FolioPalette("Ember", Color(0xFFC2552E), Color(0xFF8E5A3B), Color(0xFFB08540)),
        FolioPalette("Tide", Color(0xFF1D6F8C), Color(0xFF3C7C6C), Color(0xFF335F9E)),
        FolioPalette("Fern", Color(0xFF4B6B3B), Color(0xFF7C6B31), Color(0xFF2F6B5A)),
        FolioPalette("Iris", Color(0xFF6B4B94), Color(0xFF8A5D82), Color(0xFF4B5FA8)),
        FolioPalette("Clay", Color(0xFFA55141), Color(0xFF8D6B4A), Color(0xFF7D5A52)),
        FolioPalette("Honey", Color(0xFFB17A19), Color(0xFF9B6A2E), Color(0xFF7B7A2D)),
        FolioPalette("Ink", Color(0xFF3A4A5A), Color(0xFF56606B), Color(0xFF2D4A52)),
        FolioPalette("Blossom", Color(0xFFB4476B), Color(0xFF9B5B89), Color(0xFF8B4B72)),
    )

    fun get(index: Int): FolioPalette = all[index.coerceIn(0, all.lastIndex)]
}

object ColorMath {
    /** The two ink roles: warm near-black and warm paper. */
    val inkLow: Color = Color(0xFF17130F)
    val inkHigh: Color = Color(0xFFFFF9F2)

    fun mix(a: Color, b: Color, t: Float): Color {
        val f = t.coerceIn(0f, 1f)
        return Color(
            red = a.red + (b.red - a.red) * f,
            green = a.green + (b.green - a.green) * f,
            blue = a.blue + (b.blue - a.blue) * f,
            alpha = a.alpha + (b.alpha - a.alpha) * f,
        )
    }

    fun onColor(background: Color): Color =
        if (background.luminance() > 0.42f) inkLow else inkHigh

    /**
     * The stronger of the two ink roles on [background], by measured contrast
     * ratio rather than a luminance guess. Palette accents keep the designed
     * light-on-accent look through [onColor]; this is for colours that arrive at
     * runtime — a dynamic-primary tag chip can be near black, and a fixed dark
     * label on it is invisible.
     */
    fun inkOn(background: Color): Color {
        val l = background.luminance()
        val withDark = (l + 0.05f) / (inkLow.luminance() + 0.05f)
        val withLight = (inkHigh.luminance() + 0.05f) / (l + 0.05f)
        return if (withDark >= withLight) inkLow else inkHigh
    }

    fun readable(accent: Color, dark: Boolean): Color =
        if (dark) mix(accent, Color.White, 0.45f) else mix(accent, Color.Black, 0.22f)

    fun contrast(color: Color, amount: Float, dark: Boolean): Color {
        if (abs(amount) < 0.01f) return color
        val target = if (dark) Color.White else Color.Black
        val strength = (amount.coerceIn(-1f, 1f) * 0.35f)
        return if (strength > 0) mix(color, target, strength) else mix(color, if (dark) Color.Black else Color.White, -strength)
    }
}

object FolioSchemes {

    fun build(
        palette: FolioPalette,
        dark: Boolean,
        amoled: Boolean = false,
        contrast: Float = 0f,
    ): ColorScheme {
        val paper = when {
            amoled -> Color(0xFF000000)
            dark -> Color(0xFF141210)
            else -> Color(0xFFFBF6EF)
        }
        val surface = when {
            amoled -> Color(0xFF070606)
            dark -> Color(0xFF1A1714)
            else -> Color(0xFFFFFBF5)
        }
        val surfaceLow = when {
            amoled -> Color(0xFF0A0908)
            dark -> Color(0xFF16130F)
            else -> Color(0xFFFBF6EF)
        }
        val surfaceHigh = when {
            amoled -> Color(0xFF161412)
            dark -> Color(0xFF262119)
            else -> Color(0xFFF4EAE0)
        }
        val surfaceHighest = when {
            amoled -> Color(0xFF1D1A17)
            dark -> Color(0xFF302A21)
            else -> Color(0xFFEFE4D8)
        }
        val ink = if (dark) Color(0xFFF3ECE3) else Color(0xFF1D1913)
        val inkMuted = if (dark) Color(0xFFB8ADA0) else Color(0xFF5F564A)

        val primary = if (dark) ColorMath.mix(palette.primary, Color.White, 0.28f) else palette.primary
        val secondary = if (dark) ColorMath.mix(palette.secondary, Color.White, 0.30f) else palette.secondary
        val tertiary = if (dark) ColorMath.mix(palette.tertiary, Color.White, 0.28f) else palette.tertiary

        val primaryContainer = if (dark) {
            ColorMath.mix(palette.primary, Color.Black, 0.52f)
        } else {
            ColorMath.mix(palette.primary, Color.White, 0.84f)
        }
        val secondaryContainer = if (dark) {
            ColorMath.mix(palette.secondary, Color.Black, 0.54f)
        } else {
            ColorMath.mix(palette.secondary, Color.White, 0.86f)
        }
        val tertiaryContainer = if (dark) {
            ColorMath.mix(palette.tertiary, Color.Black, 0.55f)
        } else {
            ColorMath.mix(palette.tertiary, Color.White, 0.85f)
        }

        val surfaceVariant = if (dark) Color(0xFF2A241D) else Color(0xFFF0E6DA)
        val outline = if (dark) Color(0xFF4B4238) else Color(0xFFB3A695)
        val outlineVariant = if (dark) Color(0xFF332C25) else Color(0xFFDDD0C0)

        val onSurface = ColorMath.contrast(ink, contrast, dark)
        val onSurfaceVariant = ColorMath.contrast(inkMuted, contrast, dark)

        return if (dark) {
            darkColorScheme(
                primary = primary,
                onPrimary = ColorMath.onColor(primary),
                primaryContainer = primaryContainer,
                onPrimaryContainer = ColorMath.readable(primary, dark = true),
                inversePrimary = palette.primary,
                secondary = secondary,
                onSecondary = ColorMath.onColor(secondary),
                secondaryContainer = secondaryContainer,
                onSecondaryContainer = ColorMath.readable(secondary, dark = true),
                tertiary = tertiary,
                onTertiary = ColorMath.onColor(tertiary),
                tertiaryContainer = tertiaryContainer,
                onTertiaryContainer = ColorMath.readable(tertiary, dark = true),
                background = paper,
                onBackground = onSurface,
                surface = surface,
                onSurface = onSurface,
                surfaceVariant = surfaceVariant,
                onSurfaceVariant = onSurfaceVariant,
                surfaceTint = primary,
                inverseSurface = Color(0xFFF3ECE3),
                inverseOnSurface = Color(0xFF241F19),
                error = Color(0xFFFFB4A6),
                onError = Color(0xFF561F13),
                errorContainer = Color(0xFF7A2E1E),
                onErrorContainer = Color(0xFFFFDAD1),
                outline = outline,
                outlineVariant = outlineVariant,
                scrim = Color.Black,
                surfaceBright = ColorMath.mix(surface, Color.White, 0.20f),
                surfaceDim = ColorMath.mix(surface, Color.Black, 0.35f),
                surfaceContainerLowest = surfaceLow,
                surfaceContainerLow = ColorMath.mix(surface, Color.White, 0.06f),
                surfaceContainer = ColorMath.mix(surface, Color.White, 0.10f),
                surfaceContainerHigh = surfaceHigh,
                surfaceContainerHighest = surfaceHighest,
            )
        } else {
            lightColorScheme(
                primary = primary,
                onPrimary = ColorMath.onColor(primary),
                primaryContainer = primaryContainer,
                onPrimaryContainer = ColorMath.readable(primary, dark = false),
                inversePrimary = ColorMath.mix(primary, Color.White, 0.30f),
                secondary = secondary,
                onSecondary = ColorMath.onColor(secondary),
                secondaryContainer = secondaryContainer,
                onSecondaryContainer = ColorMath.readable(secondary, dark = false),
                tertiary = tertiary,
                onTertiary = ColorMath.onColor(tertiary),
                tertiaryContainer = tertiaryContainer,
                onTertiaryContainer = ColorMath.readable(tertiary, dark = false),
                background = paper,
                onBackground = onSurface,
                surface = surface,
                onSurface = onSurface,
                surfaceVariant = surfaceVariant,
                onSurfaceVariant = onSurfaceVariant,
                surfaceTint = primary,
                inverseSurface = Color(0xFF2B251E),
                inverseOnSurface = Color(0xFFF6EFE6),
                error = Color(0xFFB3261E),
                onError = Color(0xFFFFFFFF),
                errorContainer = Color(0xFFFFDAD4),
                onErrorContainer = Color(0xFF410E0B),
                outline = outline,
                outlineVariant = outlineVariant,
                scrim = Color.Black,
                surfaceBright = Color.White,
                surfaceDim = ColorMath.mix(paper, Color.Black, 0.10f),
                surfaceContainerLowest = Color(0xFFFFFFFF),
                surfaceContainerLow = ColorMath.mix(surface, paper, 0.45f),
                surfaceContainer = surfaceVariant,
                surfaceContainerHigh = surfaceHigh,
                surfaceContainerHighest = surfaceHighest,
            )
        }
    }

    /** Washes used by note cards and tag tiles. */
    fun noteTints(dark: Boolean): List<Color> = if (dark) {
        listOf(
            Color(0xFF3A2A1E), Color(0xFF1C2F38), Color(0xFF22301F), Color(0xFF2E2438),
            Color(0xFF382422), Color(0xFF332C1C), Color(0xFF222B33), Color(0xFF382230),
        )
    } else {
        listOf(
            Color(0xFFFBE7D4), Color(0xFFDCECEF), Color(0xFFE4EEDC), Color(0xFFEAE1F3),
            Color(0xFFF6DED8), Color(0xFFF4E9CE), Color(0xFFDEE7EE), Color(0xFFF8DDE6),
        )
    }

    fun tintAt(index: Int, dark: Boolean): Color {
        val tints = noteTints(dark)
        if (index < 0) return Color.Transparent
        return tints[index % tints.size]
    }

    fun tagColor(tag: String, dark: Boolean): Color {
        val hue = (abs(tag.lowercase().hashCode()) % 360).toFloat()
        return if (dark) {
            Color.hsv(hue, 0.34f, 0.52f)
        } else {
            Color.hsv(hue, 0.40f, 0.94f)
        }
    }
}
