package com.folio.notes

import com.folio.notes.logic.HomeLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeLayoutTest {

    @Test
    fun `phone width picks two columns`() {
        assertEquals(2, HomeLayout.gridColumns(availableWidthDp = 360f, preference = HomeLayout.AUTO))
        assertEquals(2, HomeLayout.gridColumns(availableWidthDp = 599f, preference = HomeLayout.AUTO))
    }

    @Test
    fun `medium window picks three columns`() {
        assertEquals(3, HomeLayout.gridColumns(availableWidthDp = 600f, preference = HomeLayout.AUTO))
        assertEquals(3, HomeLayout.gridColumns(availableWidthDp = 839f, preference = HomeLayout.AUTO))
    }

    @Test
    fun `expanded window picks four columns`() {
        assertEquals(4, HomeLayout.gridColumns(availableWidthDp = 840f, preference = HomeLayout.AUTO))
        assertEquals(4, HomeLayout.gridColumns(availableWidthDp = 1280f, preference = HomeLayout.AUTO))
    }

    @Test
    fun `fixed preference always wins`() {
        for (columns in HomeLayout.MIN_FIXED..HomeLayout.MAX_FIXED) {
            assertEquals(columns, HomeLayout.gridColumns(99f, preference = columns))
            assertEquals(columns, HomeLayout.gridColumns(1600f, preference = columns))
        }
    }

    @Test
    fun `out of range preference falls back to auto`() {
        assertEquals(2, HomeLayout.gridColumns(360f, preference = 0))
        assertEquals(2, HomeLayout.gridColumns(360f, preference = 1))
        assertEquals(2, HomeLayout.gridColumns(360f, preference = 7))
        assertTrue(HomeLayout.isAuto(7))
    }

    @Test
    fun `isAuto only accepts the sentinel and fixed values are not auto`() {
        assertTrue(HomeLayout.isAuto(HomeLayout.AUTO))
        assertFalse(HomeLayout.isAuto(HomeLayout.MIN_FIXED))
        assertFalse(HomeLayout.isAuto(HomeLayout.MAX_FIXED))
    }

    @Test
    fun `card width removes feed padding and gaps`() {
        assertEquals(170.5f, HomeLayout.cardWidthDp(393f, 2), 0.01f)
        assertEquals(109.67f, HomeLayout.cardWidthDp(393f, 3), 0.01f)
        assertEquals(191f, HomeLayout.cardWidthDp(840f, 4), 0.01f)
    }

    @Test
    fun `three columns on a phone are narrow but tablet columns are not`() {
        assertTrue(HomeLayout.isNarrowGrid(393f, 3))
        assertTrue(HomeLayout.isNarrowGrid(393f, 4))
        assertFalse(HomeLayout.isNarrowGrid(393f, 2))
        assertFalse(HomeLayout.isNarrowGrid(600f, 3))
        assertFalse(HomeLayout.isNarrowGrid(840f, 4))
    }
}
