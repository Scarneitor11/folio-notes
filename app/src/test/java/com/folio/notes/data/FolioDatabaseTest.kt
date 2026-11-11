package com.folio.notes.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FolioDatabaseTest {

    private lateinit var database: FolioDatabase

    @Before
    fun setUp() {
        database = FolioDatabase(ApplicationProvider.getApplicationContext<Context>())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `upsert round-trips every field`() {
        val note = Note(
            id = "note-1",
            title = "Title",
            body = "Body\nwith lines",
            tint = 3,
            pinned = true,
            archived = true,
            trashed = false,
            tags = listOf("alpha", "beta"),
            createdAt = 11L,
            updatedAt = 22L,
        )
        database.upsert(note)

        assertEquals(listOf(note), database.loadAll())
    }

    @Test
    fun `upsert replaces an existing row`() {
        database.upsert(Note(id = "same", title = "first", updatedAt = 1L))
        database.upsert(Note(id = "same", title = "second", updatedAt = 2L))

        assertEquals("second", database.loadAll().single().title)
    }

    @Test
    fun `deleteTrashed leaves live notes alone`() {
        database.upsert(Note(id = "live", title = "live"))
        database.upsert(Note(id = "gone", title = "gone", trashed = true))

        database.deleteTrashed()

        assertEquals(listOf("live"), database.loadAll().map { it.id })
    }

    @Test
    fun `replaceAll swaps the whole library`() {
        database.upsert(Note(id = "old", title = "old"))
        database.replaceAll(listOf(Note(id = "a"), Note(id = "b")))

        assertEquals(setOf("a", "b"), database.loadAll().map { it.id }.toSet())
    }

    @Test
    fun `upgrade rebuilds the table without losing notes`() {
        val note = Note(
            id = "keep",
            title = "Keep me",
            body = "Still here",
            tint = 2,
            pinned = true,
            tags = listOf("a", "b"),
            createdAt = 111L,
            updatedAt = 222L,
        )
        database.upsert(note)

        database.onUpgrade(database.writableDatabase, 1, 2)

        assertEquals(listOf(note), database.loadAll())
    }

    @Test
    fun `upgrade recreates the updated index`() {
        database.onUpgrade(database.writableDatabase, 1, 2)

        val present = database.readableDatabase.rawQuery(
            "SELECT 1 FROM sqlite_master WHERE type = 'index' AND name = 'idx_notes_updated'",
            null,
        ).use { it.moveToFirst() }

        assertTrue(present)
    }
}
