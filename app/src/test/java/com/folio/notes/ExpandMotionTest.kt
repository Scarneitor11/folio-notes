package com.folio.notes

import androidx.compose.ui.geometry.Rect
import com.folio.notes.ui.components.expandBounds
import com.folio.notes.ui.components.expandEase
import com.folio.notes.ui.components.expandRadius
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The editor's "unfold out of the tile" entrance is pure geometry, so it can be
 * proven without a device: the first frame must be exactly the tile the user
 * tapped, and the last frame must be the whole window, or the reveal ends with a
 * visible jump.
 */
class ExpandMotionTest {

    private val width = 1080f
    private val height = 2400f

    // A two-column tile on a 393 dp phone, in pixels.
    private val tile = Rect(left = 100f, top = 400f, right = 460f, bottom = 900f)

    private fun boundsAt(progress: Float): Rect =
        expandBounds(tile, width, height, expandEase(progress))

    @Test
    fun firstFrame_isExactlyTheTile() {
        val bounds = boundsAt(0f)
        assertEquals(tile.left, bounds.left, 0.5f)
        assertEquals(tile.top, bounds.top, 0.5f)
        assertEquals(tile.right, bounds.right, 0.5f)
        assertEquals(tile.bottom, bounds.bottom, 0.5f)
    }

    @Test
    fun lastFrame_isTheWholeWindow() {
        val bounds = boundsAt(1f)
        assertEquals(0f, bounds.left, 0.5f)
        assertEquals(0f, bounds.top, 0.5f)
        assertEquals(width, bounds.right, 0.5f)
        assertEquals(height, bounds.bottom, 0.5f)
    }

    @Test
    fun reveal_growsOutwardOnEveryEdgeWithoutJumpingBackwards() {
        var previous = boundsAt(0f)
        for (step in 1..40) {
            val bounds = boundsAt(step / 40f)
            assertTrue("left must not move right at $step", bounds.left <= previous.left + 0.01f)
            assertTrue("top must not move down at $step", bounds.top <= previous.top + 0.01f)
            assertTrue("right must not move left at $step", bounds.right >= previous.right - 0.01f)
            assertTrue("bottom must not move up at $step", bounds.bottom >= previous.bottom - 0.01f)
            previous = bounds
        }
    }

    @Test
    fun reveal_neverLeavesTheWindow() {
        for (step in 0..40) {
            val bounds = boundsAt(step / 40f)
            assertTrue("left edge at $step", bounds.left >= -0.5f)
            assertTrue("top edge at $step", bounds.top >= -0.5f)
            assertTrue("right edge at $step", bounds.right <= width + 0.5f)
            assertTrue("bottom edge at $step", bounds.bottom <= height + 0.5f)
        }
    }

    @Test
    fun reveal_keepsTheTileCentreOnTheSameAxis() {
        // The clip grows toward each corner of the window, so the tile's midpoint
        // drifts to the window's midpoint; it must never wander off it.
        val start = boundsAt(0f).center
        val end = boundsAt(1f).center
        val mid = boundsAt(0.5f).center
        assertTrue("centre must stay between the two", mid.x in minOf(start.x, end.x)..maxOf(start.x, end.x))
        assertTrue("centre must stay between the two", mid.y in minOf(start.y, end.y)..maxOf(start.y, end.y))
    }

    @Test
    fun ease_startsAtZeroEndsAtOneAndNeverOvershoots() {
        assertEquals(0f, expandEase(0f), 0f)
        assertEquals(1f, expandEase(1f), 0f)
        var previous = -1f
        for (step in 0..100) {
            val value = expandEase(step / 100f)
            assertTrue("eased value must be monotonic at $step", value >= previous)
            assertTrue("eased value must stay in range at $step", value in 0f..1f)
            previous = value
        }
    }

    @Test
    fun ease_clampsProgressOutsideZeroToOne() {
        assertEquals(0f, expandEase(-3f), 0f)
        assertEquals(1f, expandEase(7f), 0f)
    }

    @Test
    fun ease_frontLoadsTheMotion() {
        // Half the time budget must already deliver most of the reveal, otherwise a
        // page that is opening feels like it is stuck.
        assertTrue(expandEase(0.5f) > 0.8f)
    }

    @Test
    fun cornerRadius_fadesFromTheTileRoundnessToSquare() {
        assertEquals(28f, expandRadius(28f, 0f), 0.0001f)
        assertEquals(0f, expandRadius(28f, 1f), 0.0001f)
        assertTrue(expandRadius(28f, 0.5f) < expandRadius(28f, 0.25f))
    }

    @Test
    fun cornerRadius_clampsProgress() {
        assertEquals(28f, expandRadius(28f, -1f), 0.0001f)
        assertEquals(0f, expandRadius(28f, 4f), 0.0001f)
    }
}
