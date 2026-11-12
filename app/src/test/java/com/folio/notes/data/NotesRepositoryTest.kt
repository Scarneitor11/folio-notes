package com.folio.notes.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NotesRepositoryTest {

    private lateinit var database: FolioDatabase
    private lateinit var scope: CoroutineScope
    private lateinit var repository: NotesRepository
    private var clock = 1_000L

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = FolioDatabase(context)
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        repository = NotesRepository(
            database = database,
            scope = scope,
            clock = { clock },
            io = Dispatchers.Unconfined,
        )
    }

    @After
    fun tearDown() {
        scope.cancel()
        database.close()
    }

    @Test
    fun `save stamps updatedAt and reaches sqlite`() {
        clock = 4_200L
        repository.save(Note(id = "a", title = "Title", body = "Body", updatedAt = 1L))

        val stored = database.loadAll().single()
        assertEquals("Title", stored.title)
        assertEquals(4_200L, stored.updatedAt)
    }

    @Test
    fun `rapid saves keep the last write`() {
        repository.save(Note(id = "a", title = "v1"))
        repository.save(Note(id = "a", title = "v2"))
        repository.save(Note(id = "a", title = "v3"))

        assertEquals("v3", database.loadAll().single().title)
    }

    @Test
    fun `create is persisted and can be removed again`() {
        val note = repository.create()
        assertEquals(1, database.loadAll().size)

        repository.deleteForever(note.id)

        assertTrue(database.loadAll().isEmpty())
    }

    @Test
    fun `deleteTag rewrites only the notes that carry it`() {
        repository.save(Note(id = "tagged", title = "tagged", tags = listOf("keep", "drop")))
        repository.save(Note(id = "other", title = "other", tags = listOf("keep")))
        val untouched = repository.note("other")

        clock = 9_000L
        repository.deleteTag("drop")

        assertEquals(listOf("keep"), repository.note("tagged")!!.tags)
        assertEquals(untouched, repository.note("other"))
        assertEquals(9_000L, repository.note("tagged")!!.updatedAt)
        assertEquals(listOf("keep"), database.loadAll().first { it.id == "other" }.tags)
    }

    @Test
    fun `renameTag keeps tags unique and persists the change`() {
        repository.save(Note(id = "a", title = "a", tags = listOf("old")))
        repository.save(Note(id = "b", title = "b", tags = listOf("old", "new")))

        repository.renameTag("old", "new")

        assertEquals(listOf("new"), repository.note("a")!!.tags)
        assertEquals(listOf("new"), repository.note("b")!!.tags)
        assertEquals(listOf("new"), database.loadAll().first { it.id == "b" }.tags)
    }

    @Test
    fun `archiveMany updates only selected notes and persists them`() {
        repository.save(Note(id = "a", title = "a"))
        repository.save(Note(id = "b", title = "b"))
        repository.save(Note(id = "c", title = "c"))
        clock = 12_345L

        repository.archiveMany(setOf("a", "c"))

        assertTrue(repository.note("a")!!.archived)
        assertFalse(repository.note("b")!!.archived)
        assertTrue(repository.note("c")!!.archived)
        assertEquals(12_345L, repository.note("a")!!.updatedAt)
        assertEquals(2, database.loadAll().count { it.archived })
    }

    @Test
    fun `appendAll ignores existing and repeated ids`() {
        repository.save(Note(id = "existing", title = "existing"))

        repository.appendAll(
            listOf(
                Note(id = "existing", title = "replacement"),
                Note(id = "new", title = "new"),
                Note(id = "new", title = "duplicate"),
            ),
        )

        assertEquals(listOf("existing", "new"), repository.notes.value.map { it.id }.sorted())
        assertEquals(listOf("existing", "new"), database.loadAll().map { it.id }.sorted())
        assertEquals("new", repository.note("new")!!.title)
    }

    @Test
    fun `load hydrates the flow from disk`() {
        database.upsert(Note(id = "stored", title = "From disk", createdAt = 1L, updatedAt = 2L))

        repository.load()

        assertTrue(repository.loaded.value)
        assertEquals("From disk", repository.notes.value.single().title)
    }
}
