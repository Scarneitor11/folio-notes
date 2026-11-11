package com.folio.notes.logic

/**
 * Column count for the home feed.
 *
 * A fixed preference always wins; `AUTO` follows Material's window size classes
 * so a phone keeps a dense two-up feed while a tablet or unfolded device spreads
 * the same cards out instead of stretching two oversized ones.
 */
object HomeLayout {
    /** Preference value meaning "decide from the window width". */
    const val AUTO = 0
    const val MIN_FIXED = 2
    const val MAX_FIXED = 4

    /** Above this width the list view is centred instead of spanning the window. */
    const val LIST_MAX_WIDTH_DP = 760

    /**
     * Below this card width a note tile switches to its narrow layout: smaller
     * title, fewer preview lines, one tag and the pin beside the date.
     */
    const val MIN_CARD_WIDTH_DP = 128f

    /** Horizontal padding on each side of the home feed. */
    const val FEED_PADDING_DP = 20f

    /** Gap between grid columns. */
    const val GRID_GAP_DP = 12f

    fun gridColumns(availableWidthDp: Float, preference: Int): Int = when {
        preference in MIN_FIXED..MAX_FIXED -> preference
        availableWidthDp < 600f -> 2
        availableWidthDp < 840f -> 3
        else -> 4
    }

    /** Width of one grid card once padding and gaps are removed. */
    fun cardWidthDp(availableWidthDp: Float, columns: Int): Float {
        val safeColumns = columns.coerceAtLeast(1)
        return (availableWidthDp - 2f * FEED_PADDING_DP - GRID_GAP_DP * (safeColumns - 1)) / safeColumns
    }

    /** True when the cards in this grid are too narrow for the full tile layout. */
    fun isNarrowGrid(availableWidthDp: Float, columns: Int): Boolean =
        cardWidthDp(availableWidthDp, columns) < MIN_CARD_WIDTH_DP

    /** True when [gridColumns] is choosing the count rather than the user. */
    fun isAuto(preference: Int): Boolean = preference !in MIN_FIXED..MAX_FIXED
}
