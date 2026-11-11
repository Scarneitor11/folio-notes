package com.folio.notes

import androidx.compose.ui.geometry.Rect
import com.folio.notes.ui.Screen
import com.folio.notes.ui.opensFromTile
import com.folio.notes.ui.screenDepth
import com.folio.notes.ui.screenMovesForward
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Direction rules for the spatial navigation. They live outside the composable so a
 * wrong sign (a screen leaving the way it came in) fails here instead of showing up
 * as a reversed page slide on a device.
 */
class NavigationTest {

    private val tile = Rect(0f, 0f, 360f, 500f)

    @Test
    fun depth_ordersHomeUnderTabsUnderEditor() {
        assertEquals(0, screenDepth(Screen.Home))
        assertEquals(1, screenDepth(Screen.Tags))
        assertEquals(1, screenDepth(Screen.Settings))
        assertEquals(2, screenDepth(Screen.Editor("note-1")))
        assertEquals(2, screenDepth(Screen.Editor("note-1", origin = tile)))
    }

    @Test
    fun forward_push_movesDeeperIntoTheStack() {
        assertTrue(screenMovesForward(Screen.Home, Screen.Settings))
        assertTrue(screenMovesForward(Screen.Home, Screen.Tags))
        assertTrue(screenMovesForward(Screen.Home, Screen.Editor("note-1")))
        assertTrue(screenMovesForward(Screen.Tags, Screen.Editor("note-1")))
    }

    @Test
    fun backward_pop_returnsThroughTheSameDoor() {
        assertFalse(screenMovesForward(Screen.Settings, Screen.Home))
        assertFalse(screenMovesForward(Screen.Tags, Screen.Home))
        assertFalse(screenMovesForward(Screen.Editor("note-1"), Screen.Home))
        assertFalse(screenMovesForward(Screen.Editor("note-1"), Screen.Settings))
    }

    @Test
    fun siblingTabs_keepPushingForward() {
        // Tags -> Settings is lateral, but treating it as a push keeps the incoming
        // screen entering from one side instead of two screens crossing.
        assertTrue(screenMovesForward(Screen.Tags, Screen.Settings))
        assertTrue(screenMovesForward(Screen.Settings, Screen.Tags))
    }

    @Test
    fun openingFromATile_unfoldsInsteadOfPanning() {
        assertTrue(opensFromTile(Screen.Editor("note-1", origin = tile)))
        assertFalse(opensFromTile(Screen.Editor("note-1")))
        assertFalse(opensFromTile(Screen.Home))
        assertFalse(opensFromTile(Screen.Settings))
    }

    @Test
    fun originIsPartOfTheRoute_soATileOpenAndASheetOpenAreDifferentTransitions() {
        assertNotEquals(Screen.Editor("note-1"), Screen.Editor("note-1", origin = tile))
        assertEquals(Screen.Editor("note-1", origin = tile), Screen.Editor("note-1", origin = tile))
    }
}
