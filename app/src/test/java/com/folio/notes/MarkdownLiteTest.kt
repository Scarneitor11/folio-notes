package com.folio.notes

import com.folio.notes.logic.Block
import com.folio.notes.logic.MarkStyle
import com.folio.notes.logic.MarkdownLite
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownLiteTest {

    @Test
    fun headingsCarryTheirLevel() {
        val blocks = MarkdownLite.parse("# One\n\n### Three")
        assertEquals(2, blocks.size)
        assertEquals(1, (blocks[0] as Block.Heading).level)
        assertEquals(3, (blocks[1] as Block.Heading).level)
        assertEquals("One", blocks[0].let { (it as Block.Heading).spans.first().text })
    }

    @Test
    fun headingNeedsASpaceAfterTheHashes() {
        val blocks = MarkdownLite.parse("#nope")
        assertTrue(blocks.single() is Block.Paragraph)
    }

    @Test
    fun tasksBecomeBulletsWithStateAndSourceLine() {
        val markdown = "intro\n\n- [x] done\n- [ ] waiting\n  - nested"
        val bullets = MarkdownLite.parse(markdown).filterIsInstance<Block.Bullet>()
        assertEquals(3, bullets.size)
        assertEquals(true, bullets[0].checked)
        assertEquals(false, bullets[1].checked)
        assertEquals(null, bullets[2].checked)
        assertEquals(1, bullets[2].indent)
        // Line indexes point at the original source so the checkbox can be flipped.
        assertEquals(2, bullets[0].line)
        assertEquals(4, bullets[2].line)
    }

    @Test
    fun codeFenceKeepsItsContentsVerbatim() {
        val blocks = MarkdownLite.parse("```kt\nfun x() = 1\n\nfun y() = 2\n```")
        val code = blocks.single() as Block.Code
        assertEquals("fun x() = 1\n\nfun y() = 2", code.code)
    }

    @Test
    fun rulesQuotesAndParagraphsAreSeparated() {
        val blocks = MarkdownLite.parse("first line\nsecond line\n\n---\n\n> quoted")
        assertEquals(3, blocks.size)
        assertTrue(blocks[0] is Block.Paragraph)
        assertTrue(blocks[1] is Block.Rule)
        assertTrue(blocks[2] is Block.Quote)
        // Consecutive lines join into one paragraph.
        assertEquals(1, (blocks[0] as Block.Paragraph).spans.size)
        assertEquals("first line second line", (blocks[0] as Block.Paragraph).spans[0].text)
    }

    @Test
    fun inlineStylesAreDetected() {
        val spans = MarkdownLite.inline("**bold** *italic* `code` ~~gone~~")
            .filter { it.text.isNotBlank() }
        assertEquals(4, spans.size)
        assertEquals("bold", spans[0].text)
        assertEquals(MarkStyle.BOLD, spans[0].style)
        assertEquals("italic", spans[1].text)
        assertEquals(MarkStyle.ITALIC, spans[1].style)
        assertEquals(MarkStyle.CODE, spans[2].style)
        assertEquals(MarkStyle.STRIKE, spans[3].style)
    }

    @Test
    fun boldWinsOverItalicWhenNested() {
        val spans = MarkdownLite.inline("**bold**")
        assertEquals(MarkStyle.BOLD, spans.single().style)
    }

    @Test
    fun unmatchedMarkersStayLiteral() {
        val spans = MarkdownLite.inline("2 * 3 = 6")
        assertEquals("2 * 3 = 6", spans.joinToString("") { it.text })
        assertEquals(MarkStyle.NORMAL, spans.single().style)
    }

    @Test
    fun escapedMarkerIsNotStyled() {
        val spans = MarkdownLite.inline("\\*not italic\\*")
        assertEquals("*not italic*", spans.single().text)
        assertEquals(MarkStyle.NORMAL, spans.single().style)
    }

    @Test
    fun plainTextDropsMarkersButKeepsWords() {
        val text = MarkdownLite.plainText("# Title\n\n- [x] **done** thing")
        assertEquals("Title\n☑\u00A0 done thing", text)
    }

    @Test
    fun plainTextKeepsAGapAfterTaskGlyphs() {
        assertEquals("☐\u00A0 open", MarkdownLite.plainText("- [ ] open"))
        assertEquals("☑\u00A0 done", MarkdownLite.plainText("- [x] done"))
        assertEquals("•\u00A0 bullet", MarkdownLite.plainText("- bullet"))
    }

    @Test
    fun firstLineSkipsLeadingBlankBlocks() {
        assertEquals("Real start", MarkdownLite.firstLine("\n\n---\n\nReal start\n\nmore"))
    }

    @Test
    fun firstLineOfEmptyMarkdownIsEmpty() {
        assertEquals("", MarkdownLite.firstLine("   \n\n"))
    }

    @Test
    fun toggleCheckboxFlipsOnlyTheTargetLine() {
        val source = "- [ ] a\n- [ ] b"
        val flipped = MarkdownLite.toggleCheckbox(source, 1)
        assertEquals("- [ ] a\n- [x] b", flipped)
        assertEquals(source, MarkdownLite.toggleCheckbox(flipped, 1))
    }

    @Test
    fun toggleCheckboxPreservesIndentAndTrailingText() {
        val flipped = MarkdownLite.toggleCheckbox("  - [x] nested  ", 0)
        assertEquals("  - [ ] nested  ", flipped)
    }

    @Test
    fun toggleCheckboxIgnoresLinesWithoutACheckbox() {
        val source = "just a paragraph"
        assertEquals(source, MarkdownLite.toggleCheckbox(source, 0))
        assertEquals(source, MarkdownLite.toggleCheckbox(source, 9))
    }
}
