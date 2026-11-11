package com.folio.notes

import com.folio.notes.data.Feed
import com.folio.notes.data.Note
import com.folio.notes.data.SortMode
import com.folio.notes.logic.NoteQuery
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteQueryTest {

    private fun note(
        id: String,
        title: String = "",
        body: String = "",
        pinned: Boolean = false,
        archived: Boolean = false,
        trashed: Boolean = false,
        tags: List<String> = emptyList(),
        created: Long = 0L,
        updated: Long = 0L,
    ) = Note(
        id = id,
        title = title,
        body = body,
        pinned = pinned,
        archived = archived,
        trashed = trashed,
        tags = tags,
        createdAt = created,
        updatedAt = updated,
    )

    private val sample = listOf(
        note("a", title = "Alpha", tags = listOf("work"), created = 1, updated = 30),
        note("b", title = "Beta", pinned = true, tags = listOf("work", "home"), created = 2, updated = 10),
        note("c", title = "Gamma", archived = true, updated = 20),
        note("d", title = "Delta", trashed = true, updated = 40),
    )

    @Test
    fun allFeedHidesArchivedAndTrashed() {
        val ids = NoteQuery.visible(sample, Feed.ALL, SortMode.UPDATED_DESC).map { it.id }
        assertEquals(listOf("b", "a"), ids)
    }

    @Test
    fun pinnedFeedOnlyPinned() {
        val ids = NoteQuery.visible(sample, Feed.PINNED, SortMode.UPDATED_DESC).map { it.id }
        assertEquals(listOf("b"), ids)
    }

    @Test
    fun archivedFeedOnlyArchived() {
        val ids = NoteQuery.visible(sample, Feed.ARCHIVED, SortMode.UPDATED_DESC).map { it.id }
        assertEquals(listOf("c"), ids)
    }

    @Test
    fun trashFeedOnlyTrashed() {
        val ids = NoteQuery.visible(sample, Feed.TRASH, SortMode.UPDATED_DESC).map { it.id }
        assertEquals(listOf("d"), ids)
    }

    @Test
    fun searchMatchesTitleBodyAndTagsCaseInsensitively() {
        val notes = sample + note("e", title = "", body = "the QUICK brown fox", tags = emptyList())
        assertEquals(
            listOf("e"),
            NoteQuery.visible(notes, Feed.ALL, SortMode.UPDATED_DESC, query = "quick").map { it.id },
        )
        assertEquals(
            listOf("b", "a"),
            NoteQuery.visible(notes, Feed.ALL, SortMode.UPDATED_DESC, query = "WORK").map { it.id },
        )
    }

    @Test
    fun tagFilterIsExactAndCaseInsensitive() {
        val ids = NoteQuery.visible(sample, Feed.ALL, SortMode.UPDATED_DESC, tag = "HOME").map { it.id }
        assertEquals(listOf("b"), ids)
        assertTrue(
            NoteQuery.visible(sample, Feed.ALL, SortMode.UPDATED_DESC, tag = "hom").isEmpty(),
        )
    }

    @Test
    fun pinnedNotesFloatToTheTopOfTheFeed() {
        val notes = listOf(
            note("fresh", updated = 100),
            note("stale", pinned = true, updated = 1),
        )
        assertEquals(
            listOf("stale", "fresh"),
            NoteQuery.visible(notes, Feed.ALL, SortMode.UPDATED_DESC).map { it.id },
        )
    }

    @Test
    fun sortModesOrderAsAdvertised() {
        assertEquals(listOf("d", "a", "c", "b"), NoteQuery.sorted(sample, SortMode.UPDATED_DESC).map { it.id })
        assertEquals(listOf("b", "c", "a", "d"), NoteQuery.sorted(sample, SortMode.UPDATED_ASC).map { it.id })
        assertEquals(listOf("b", "a", "c", "d"), NoteQuery.sorted(sample, SortMode.CREATED_DESC).map { it.id })
        assertEquals(listOf("a", "b", "d", "c"), NoteQuery.sorted(sample, SortMode.TITLE_ASC).map { it.id })
        assertEquals(listOf("c", "d", "b", "a"), NoteQuery.sorted(sample, SortMode.TITLE_DESC).map { it.id })
    }

    @Test
    fun titleFallsBackToFirstBodyLineThenUntitled() {
        assertEquals("Alpha", NoteQuery.titleOf(note("x", title = "Alpha")))
        assertEquals("From body", NoteQuery.titleOf(note("x", body = "# From body\nrest")))
        assertEquals("Untitled", NoteQuery.titleOf(note("x")))
    }

    @Test
    fun previewStripsMarkdownAndSkipsTheTitleLine() {
        val body = "Body title\n\n**bold** and `code`"
        assertEquals("bold and code", NoteQuery.previewOf(note("x", body = body)))
        assertEquals("Body title\nbold and code", NoteQuery.previewOf(note("x", title = "T", body = body)))
    }

    @Test
    fun previewIsTruncated() {
        val long = "Heading\n" + "word ".repeat(200)
        val preview = NoteQuery.previewOf(note("x", body = long), maxChars = 40)
        assertTrue(preview.isNotEmpty())
        assertTrue(preview.length <= 40)
    }

    @Test
    fun allTagsCountsIgnoresTrashAndSortsByCount() {
        val tags = NoteQuery.allTags(sample)
        assertEquals(listOf("work" to 2, "home" to 1), tags)
    }

    @Test
    fun wordsAndReadingTime() {
        assertEquals(0, NoteQuery.words("   \n\t "))
        assertEquals(3, NoteQuery.words("one two three"))
        assertEquals(0, NoteQuery.readingMinutes(""))
        assertEquals(1, NoteQuery.readingMinutes("short note"))
        assertEquals(2, NoteQuery.readingMinutes("w ".repeat(400)))
    }

    @Test
    fun relativeTimeBuckets() {
        val now = 1_000_000_000_000L
        assertEquals("just now", NoteQuery.relativeTime(now, now - 30_000))
        assertEquals("5m ago", NoteQuery.relativeTime(now, now - 5 * 60_000))
        assertEquals("3h ago", NoteQuery.relativeTime(now, now - 3 * 3_600_000))
        assertEquals("2d ago", NoteQuery.relativeTime(now, now - 2 * 86_400_000L))
        assertEquals("2w ago", NoteQuery.relativeTime(now, now - 14 * 86_400_000L))
        assertEquals("", NoteQuery.relativeTime(now, 0L))
    }

    @Test
    fun relativeTimeHandlesClockSkew() {
        assertEquals("just now", NoteQuery.relativeTime(1_000L, 5_000L))
    }

    @Test
    fun sameDayIgnoresTimeOfDay() {
        // Built in the local zone so the assertion does not depend on the runner's timezone.
        val morning = java.util.Calendar.getInstance().apply {
            set(2024, java.util.Calendar.MAY, 12, 9, 0, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
        assertTrue(NoteQuery.sameDay(morning, morning + 3_600_000L))
        assertTrue(!NoteQuery.sameDay(morning, morning + 86_400_000L * 2))
    }
}
