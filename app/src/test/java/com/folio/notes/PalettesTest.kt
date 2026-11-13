package com.folio.notes

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import com.folio.notes.logic.ColorMath
import com.folio.notes.logic.FolioSchemes
import com.folio.notes.logic.Palettes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PalettesTest {

    private val ember = Palettes.get(0)

    @Test
    fun allPalettesAreDistinctAndNamed() {
        assertEquals(8, Palettes.all.size)
        assertEquals(8, Palettes.all.map { it.name }.toSet().size)
        assertTrue(Palettes.all.none { it.name.isBlank() })
    }

    @Test
    fun getClampsTheIndex() {
        assertEquals(Palettes.all.first(), Palettes.get(-4))
        assertEquals(Palettes.all.last(), Palettes.get(99))
        assertEquals(Palettes.all[3], Palettes.get(3))
    }

    @Test
    fun mixHitsBothEndpoints() {
        val a = Color(0xFF102030)
        val b = Color(0xFFA0B0C0)
        assertEquals(a.toArgb(), ColorMath.mix(a, b, 0f).toArgb())
        assertEquals(b.toArgb(), ColorMath.mix(a, b, 1f).toArgb())
        assertEquals(a.toArgb(), ColorMath.mix(a, b, -1f).toArgb())
        assertEquals(b.toArgb(), ColorMath.mix(a, b, 5f).toArgb())
    }

    @Test
    fun mixWalksTheMiddle() {
        val mid = ColorMath.mix(Color.Black, Color.White, 0.5f)
        assertTrue(mid.luminance() in 0.15f..0.30f)
    }

    @Test
    fun onColorFlipsForLightAndDarkBackgrounds() {
        assertEquals(0xFF17130F.toInt(), ColorMath.onColor(Color.White).toArgb())
        assertEquals(0xFFFFF9F2.toInt(), ColorMath.onColor(Color.Black).toArgb())
    }

    @Test
    fun inkOnReadsBlackFillWithPaper() {
        // A dynamic-colour primary can be near black; the selected chip label
        // used to be fixed dark ink and vanished into it.
        assertEquals(0xFFFFF9F2.toInt(), ColorMath.inkOn(Color.Black).toArgb())
        assertEquals(0xFFFFF9F2.toInt(), ColorMath.inkOn(Color(0xFF14100C)).toArgb())
        assertEquals(0xFF17130F.toInt(), ColorMath.inkOn(Color.White).toArgb())
    }

    @Test
    fun inkOnAlwaysBeatsOnColorOnMidTones() {
        val mids = listOf(0xFF5C97AC, 0xFF7A7A7A, 0xFF9C7A5E, 0xFF2B2B2B).map { Color(it) }
        for (bg in mids) {
            val l = bg.luminance()
            val picked = ColorMath.inkOn(bg).luminance()
            val other = if (picked > l) ColorMath.inkLow.luminance() else ColorMath.inkHigh.luminance()
            val ratio = { fg: Float -> (maxOf(l, fg) + 0.05f) / (minOf(l, fg) + 0.05f) }
            assertTrue(ratio(picked) >= ratio(other))
        }
    }

    @Test
    fun inkOnKeepsTagChipsLegibleInDarkThemes() {
        val chip = FolioSchemes.tagColor("personal", dark = true)
        val ink = ColorMath.inkOn(chip)
        val l = chip.luminance()
        val ratio = (maxOf(l, ink.luminance()) + 0.05f) / (minOf(l, ink.luminance()) + 0.05f)
        assertTrue(ratio >= 4.5f)
    }

    @Test
    fun unselectedChipLabelPassesAaOverTheCompositeFill() {
        // Unselected chips are the accent at 30% over the page. A near-black
        // dynamic primary over paper lands on mid grey, where a fixed
        // onSurfaceVariant label measured 3.9:1 on device.
        val cases = listOf(
            "light/dark-primary" to (0xFFF3F3F4 to 0xFF0C0C0C),
            "dark/light-primary" to (0xFF121414 to 0xFFF3F3F3),
            "light/accent" to (0xFFF3F3F4 to 0xFF5C97AC),
        )
        for ((name, pair) in cases) {
            val (backdrop, accent) = pair
            val painted = ColorMath.mix(Color(backdrop), Color(accent), 0.30f)
            val ink = ColorMath.inkOn(painted)
            val l = painted.luminance()
            val ratio = (maxOf(l, ink.luminance()) + 0.05f) / (minOf(l, ink.luminance()) + 0.05f)
            assertTrue("$name measured $ratio", ratio >= 4.5f)
        }
    }

    @Test
    fun contrastOfZeroIsANoOp() {
        val color = Color(0xFF335577)
        assertEquals(color.toArgb(), ColorMath.contrast(color, 0f, dark = false).toArgb())
        assertEquals(color.toArgb(), ColorMath.contrast(color, 0.001f, dark = true).toArgb())
    }

    @Test
    fun positiveContrastPushesTowardsTheForeground() {
        val base = Color(0xFF808080)
        assertTrue(ColorMath.contrast(base, 0.5f, dark = false).luminance() < base.luminance())
        assertTrue(ColorMath.contrast(base, 0.5f, dark = true).luminance() > base.luminance())
    }

    @Test
    fun readableAccentIsAlwaysDistinctFromTheAccent() {
        assertNotEquals(ember.primary.toArgb(), ColorMath.readable(ember.primary, dark = false).toArgb())
        assertNotEquals(ember.primary.toArgb(), ColorMath.readable(ember.primary, dark = true).toArgb())
    }

    @Test
    fun noteTintsWrapAndRejectNegativeIndexes() {
        val tints = FolioSchemes.noteTints(dark = false)
        assertEquals(8, tints.size)
        assertEquals(8, FolioSchemes.noteTints(dark = true).size)
        assertEquals(0, FolioSchemes.tintAt(-1, dark = false).toArgb())
        assertEquals(tints[3].toArgb(), FolioSchemes.tintAt(3, dark = false).toArgb())
        assertEquals(tints[3].toArgb(), FolioSchemes.tintAt(11, dark = false).toArgb())
    }

    @Test
    fun tagColoursAreStablePerTag() {
        assertEquals(
            FolioSchemes.tagColor("work", dark = false).toArgb(),
            FolioSchemes.tagColor("WORK", dark = false).toArgb(),
        )
        assertNotEquals(
            FolioSchemes.tagColor("work", dark = false).toArgb(),
            FolioSchemes.tagColor("work", dark = true).toArgb(),
        )
    }

    @Test
    fun lightSchemeUsesWarmPaper() {
        val scheme = FolioSchemes.build(ember, dark = false)
        assertTrue(scheme.background.luminance() > 0.9f)
        assertTrue(scheme.onBackground.luminance() < 0.2f)
        assertTrue(scheme.surface.luminance() > scheme.surfaceVariant.luminance())
    }

    @Test
    fun darkSchemeIsDark() {
        val scheme = FolioSchemes.build(ember, dark = true)
        assertTrue(scheme.background.luminance() < 0.05f)
        assertTrue(scheme.onBackground.luminance() > 0.8f)
        assertTrue(scheme.primary.luminance() > ember.primary.luminance())
    }

    @Test
    fun amoledSchemeIsTrueBlack() {
        val scheme = FolioSchemes.build(ember, dark = true, amoled = true)
        assertEquals(0xFF000000.toInt(), scheme.background.toArgb())
    }

    @Test
    fun everySchemeFillsTheContainerRoles() {
        val schemes: List<ColorScheme> = listOf(
            FolioSchemes.build(ember, dark = false),
            FolioSchemes.build(ember, dark = true),
            FolioSchemes.build(ember, dark = true, amoled = true),
        )
        schemes.forEach { scheme ->
            val containers = listOf(
                scheme.surfaceContainerLowest,
                scheme.surfaceContainerLow,
                scheme.surfaceContainer,
                scheme.surfaceContainerHigh,
                scheme.surfaceContainerHighest,
            )
            assertTrue(containers.toSet().size >= 4)
            assertNotEquals(scheme.onSurface.toArgb(), scheme.onSurfaceVariant.toArgb())
            assertNotEquals(scheme.primary.toArgb(), scheme.secondary.toArgb())
        }
    }

    @Test
    fun contrastSettingShiftsBothInkRoles() {
        val plain = FolioSchemes.build(ember, dark = false, contrast = 0f)
        val punchy = FolioSchemes.build(ember, dark = false, contrast = 0.6f)
        assertTrue(punchy.onSurface.luminance() < plain.onSurface.luminance())
        assertTrue(punchy.onSurfaceVariant.luminance() < plain.onSurfaceVariant.luminance())
    }
}
