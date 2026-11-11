package com.folio.notes

import com.folio.notes.logic.KeepImport
import com.folio.notes.logic.asFolioNote
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class KeepImportTest {

    private val keepJson = """
        {
          "color": "RED",
          "isTrashed": false,
          "isPinned": true,
          "isArchived": false,
          "title": "Weekend",
          "textContent": "A note about the weekend.",
          "listContent": [
            {"text": "milk", "isChecked": false},
            {"text": "bread", "isChecked": true}
          ],
          "labels": [{"name": "home"}, {"name": "errands"}],
          "createdTimestampUsec": 1609459200000000,
          "userEditedTimestampUsec": 1609545600000000
        }
    """.trimIndent()

    @Test
    fun jsonPreservesBodyChecklistLabelsAndMetadata() {
        val note = KeepImport.parse("Takeout/Keep/weekend.json", keepJson.toByteArray())
            .notes
            .single()

        assertEquals("Weekend", note.title)
        assertTrue(note.body.contains("A note about the weekend."))
        assertTrue(note.body.contains("- [ ] milk"))
        assertTrue(note.body.contains("- [x] bread"))
        assertEquals(listOf("home", "errands"), note.labels)
        assertTrue(note.pinned)
        assertFalse(note.archived)
        assertFalse(note.trashed)
        assertEquals(4, note.tint)
        assertEquals(1_609_459_200_000L, note.createdAt)
        assertEquals(1_609_545_600_000L, note.updatedAt)
    }

    @Test
    fun htmlPreservesTitleBodyLabelsAndEntities() {
        val html = """
            <html><head><title>fallback</title></head><body>
            <div class="note">
              <div class="title">Trip</div>
              <div class="content">Line one<br>Line &amp; two</div>
              <div class="labels">travel, ideas</div>
            </div>
            </body></html>
        """.trimIndent()
        val note = KeepImport.parse("trip.html", html.toByteArray()).notes.single()

        assertEquals("Trip", note.title)
        assertEquals("Line one\nLine & two", note.body)
        assertEquals(listOf("travel", "ideas"), note.labels)
    }

    @Test
    fun htmlChecklistItemsKeepTheirCheckedState() {
        val html = """
            <div class="title">Tasks</div>
            <div class="content"><ul>
              <li class="checked">Done</li>
              <li>Waiting</li>
            </ul></div>
        """.trimIndent()
        val body = KeepImport.parse("tasks.html", html.toByteArray()).notes.single().body

        assertTrue(body.contains("- [x] Done"))
        assertTrue(body.contains("- Waiting"))
    }

    @Test
    fun zipPrefersJsonWhenHtmlDuplicateExists() {
        val html = """<div class="title">From HTML</div><div class="content">old</div>"""
        val bytes = zip(
            "Takeout/Keep/note.json" to keepJson,
            "Takeout/Keep/note.html" to html,
        )
        val result = KeepImport.parse("takeout.zip", bytes)

        assertEquals(1, result.notes.size)
        assertEquals("Weekend", result.notes.single().title)
    }

    @Test
    fun zipUsesHtmlWhenThereIsNoJson() {
        val html = """<div class="title">HTML only</div><div class="content">still readable</div>"""
        val result = KeepImport.parse(
            "takeout.zip",
            zip("Takeout/Keep/note.html" to html, "Takeout/Keep/Labels.txt" to "home"),
        )

        assertEquals(1, result.notes.size)
        assertEquals("HTML only", result.notes.single().title)
    }

    @Test
    fun plainTextBecomesAUsableNote() {
        val result = KeepImport.parse(
            "groceries.md",
            "# Groceries\n\n- milk\n- bread".toByteArray(),
        )

        val note = result.notes.single()
        assertEquals("groceries", note.title)
        assertTrue(note.body.contains("- milk"))
    }

    @Test
    fun malformedJsonAndEmptyInputAreSkipped() {
        assertTrue(KeepImport.parse("broken.json", "{not json".toByteArray()).notes.isEmpty())
        assertTrue(KeepImport.parse("empty.txt", ByteArray(0)).notes.isEmpty())
    }

    @Test
    fun metadataMapsToAStableFolioNote() {
        val keep = KeepImport.parse("weekend.json", keepJson.toByteArray()).notes.single()
        val first = keep.asFolioNote(now = 42L)
        val second = keep.asFolioNote(now = 99L)

        assertEquals(first.id, second.id)
        assertEquals("Weekend", first.title)
        assertEquals(listOf("home", "errands"), first.tags)
        assertTrue(first.pinned)
        assertEquals(4, first.tint)
        assertEquals(1_609_459_200_000L, first.createdAt)
    }

    @Test
    fun emptyHtmlIsIgnored() {
        assertTrue(KeepImport.parse("empty.html", "<html><body></body></html>".toByteArray()).notes.isEmpty())
    }

    @Test
    fun inputWithoutContentIsNotTurnedIntoANote() {
        val result = KeepImport.parse("empty.json", """{"title":"","textContent":"","listContent":[]}""".toByteArray())
        assertTrue(result.notes.isEmpty())
        assertNotNull(result)
    }

    private fun zip(vararg entries: Pair<String, String>): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            entries.forEach { (name, content) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(content.toByteArray())
                zip.closeEntry()
            }
        }
        return out.toByteArray()
    }
}
