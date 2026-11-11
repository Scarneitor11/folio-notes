package com.folio.notes

import com.folio.notes.data.BackupCodec
import com.folio.notes.data.Note
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupCodecTest {

    private val sample = listOf(
        Note(
            id = "n1",
            title = "Shopping",
            body = "# List\n\n- [ ] milk",
            tint = 3,
            pinned = true,
            tags = listOf("home", "errands"),
            createdAt = 1_000L,
            updatedAt = 2_000L,
        ),
        Note(
            id = "n2",
            title = "",
            body = "plain body",
            trashed = true,
            createdAt = 3_000L,
            updatedAt = 4_000L,
        ),
    )

    @Test
    fun roundTripPreservesEveryField() {
        val decoded = BackupCodec.decode(BackupCodec.encode(sample, now = 42L))
        assertEquals(sample, decoded)
    }

    @Test
    fun headerNamesTheAppAndCarriesAnExportStamp() {
        val root = JSONObject(BackupCodec.encode(sample, now = 42L))
        assertEquals("Folio", root.getString("app"))
        assertEquals(42L, root.getLong("exportedAt"))
        assertEquals(1, root.getInt("version"))
        assertEquals(2, root.getJSONArray("notes").length())
    }

    @Test
    fun tagsSurviveAsAnArray() {
        val root = JSONObject(BackupCodec.encode(sample))
        val tags = root.getJSONArray("notes").getJSONObject(0).getJSONArray("tags")
        assertEquals(2, tags.length())
        assertEquals("home", tags.getString(0))
    }

    @Test
    fun emptyEncodeIsStillValidJson() {
        val decoded = BackupCodec.decode(BackupCodec.encode(emptyList()))
        assertTrue(decoded.isEmpty())
    }

    @Test
    fun decodeOfAnEmptyObjectYieldsNoNotes() {
        assertTrue(BackupCodec.decode("{}").isEmpty())
    }

    @Test
    fun decodeFillsInMissingFields() {
        val raw = """{"notes":[{"title":"Only a title"}]}"""
        val note = BackupCodec.decode(raw).single()
        assertEquals("Only a title", note.title)
        assertTrue(note.id.isNotBlank())
        assertEquals(-1, note.tint)
        assertTrue(note.tags.isEmpty())
    }

    @Test
    fun decodeGeneratesAnIdPerNoteWhenMissing() {
        val raw = """{"notes":[{},{"title":"b"}]}"""
        val notes = BackupCodec.decode(raw)
        assertEquals(2, notes.size)
        assertNotEquals(notes[0].id, notes[1].id)
    }

    @Test
    fun decodeTrimsTagsAndDropsBlanks() {
        val raw = """{"notes":[{"tags":[" work ","  ","home"]}]}"""
        assertEquals(listOf("work", "home"), BackupCodec.decode(raw).single().tags)
    }

    @Test
    fun decodeSkipsEntriesThatAreNotObjects() {
        val raw = """{"notes":["nope",{"title":"ok"}]}"""
        val notes = BackupCodec.decode(raw)
        assertEquals(1, notes.size)
        assertEquals("ok", notes.single().title)
    }
}
