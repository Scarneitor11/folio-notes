package com.folio.notes

import com.folio.notes.logic.EvernoteImport
import com.folio.notes.logic.MarkdownImport
import com.folio.notes.logic.MigrationImport
import com.folio.notes.logic.asFolioNote
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class MigrationImportTest {

    @Test
    fun markdownFrontMatterBecomesTitleTagsAndDates() {
        val source = """
            ---
            title: "Reading list"
            tags: [books, "long form"]
            created: 2024-01-02
            updated: 2024-01-03T10:30:00Z
            ---
            # Ignored heading

            Keep the actual prose.
        """.trimIndent()

        val note = MarkdownImport.parse("reading-list.md", source).notes.single()

        assertEquals("Reading list", note.title)
        assertEquals(listOf("books", "long form"), note.labels)
        assertFalse(note.body.contains("Ignored heading"))
        assertTrue(note.body.contains("Keep the actual prose."))
        assertEquals(1_704_153_600_000L, note.createdAt)
    }

    @Test
    fun markdownHeadingBecomesTitleWhenFrontMatterHasNone() {
        val note = MarkdownImport.parse(
            "trip.md",
            "# A weekend in Lisbon\n\nPastel de nata, tram 28, and rain.",
        ).notes.single()

        assertEquals("A weekend in Lisbon", note.title)
        assertTrue(note.body.contains("Pastel de nata"))
        assertFalse(note.body.startsWith("#"))
    }

    @Test
    fun markdownWithoutTitleUsesTheFileName() {
        val note = MarkdownImport.parse("quick-capture.md", "One plain thought.").notes.single()
        assertEquals("quick-capture", note.title)
        assertEquals("One plain thought.", note.body)
    }

    @Test
    fun enexPreservesTitleTagsBodyAndTimestamp() {
        val enex = """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE en-export SYSTEM "http://xml.evernote.com/pub/evernote-export3.dtd">
            <en-export export-date="20240104T120000Z" application="Evernote">
              <note>
                <title>Project atlas</title>
                <content><![CDATA[<?xml version="1.0" encoding="UTF-8"?>
                  <en-note><div>First line</div><div><en-todo checked="true"/> Ship it</div>
                  <div><en-todo/> Write notes</div><en-media hash="abc" type="image/png"/></en-note>]]></content>
                <created>20240102T101500Z</created>
                <updated>20240103T111500Z</updated>
                <tag>work</tag>
                <tag>#ideas</tag>
              </note>
            </en-export>
        """.trimIndent()

        val note = EvernoteImport.parse("atlas.enex", enex.toByteArray()).notes.single()

        assertEquals("Project atlas", note.title)
        assertEquals(listOf("work", "ideas"), note.labels)
        assertTrue(note.body.contains("First line"))
        assertTrue(note.body.contains("- [x] Ship it"))
        assertTrue(note.body.contains("- [ ] Write notes"))
        assertFalse(note.body.contains("<en-media"))
        assertEquals(1_704_190_500_000L, note.createdAt)
    }

    @Test
    fun markdownVaultZipImportsMarkdownAndSkipsConfig() {
        val zip = zip(
            "vault/.obsidian/workspace.json" to "{}",
            "vault/Projects/Atlas.md" to "---\ntags: [work]\n---\n# Atlas\n\nShip the thing.",
            "vault/Inbox/Quick note.md" to "A plain thought.",
            "vault/readme.txt" to "not a note",
        )

        val result = MarkdownImport.parseZip(zip.inputStream())

        assertEquals(2, result.notes.size)
        assertEquals("Atlas", result.notes[0].title)
        assertEquals(listOf("work"), result.notes[0].labels)
        assertEquals("Quick note", result.notes[1].title)
    }

    @Test
    fun enexMapsToAStableFolioId() {
        val enex = """
            <en-export><note><title>One</title><content>Body</content>
            <created>20240102T101500Z</created></note></en-export>
        """.trimIndent()
        val note = EvernoteImport.parse("one.enex", enex.toByteArray()).notes.single()
        assertEquals(note.asFolioNote(now = 1L).id, note.asFolioNote(now = 2L).id)
    }

    @Test
    fun routerRecognizesEnexAndFrontMatter() {
        val enex = "<en-export><note><title>Note</title><content>Text</content></note></en-export>"
        val enexResult = MigrationImport.parse("export.xml", "application/xml", enex.toByteArray())
        assertEquals("Note", enexResult.notes.single().title)

        val markdown = "---\ntitle: Hello\n---\nBody"
        val markdownResult = MigrationImport.parse("hello.md", "text/markdown", markdown.toByteArray())
        assertEquals("Hello", markdownResult.notes.single().title)
    }

    @Test
    fun malformedEnexIsRejectedInsteadOfCrashing() {
        assertTrue(EvernoteImport.parse("broken.enex", "<en-export><note>".toByteArray()).notes.isEmpty())
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
