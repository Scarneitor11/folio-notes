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
