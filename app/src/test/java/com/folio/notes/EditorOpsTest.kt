package com.folio.notes

import com.folio.notes.logic.EditorOps
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EditorOpsTest {

    @Test
    fun wrapWithNoSelectionPlacesCaretBetweenTheMarkers() {
        val edit = EditorOps.wrap("hello", 5, 5, "**")
        assertEquals("hello****", edit.text)
        assertEquals(7, edit.start)
        assertEquals(7, edit.end)
        assertEquals(7, edit.caret)
    }

    @Test
    fun wrapWithSelectionKeepsTheSelectionAroundTheText() {
        val edit = EditorOps.wrap("hello world", 0, 5, "**")
        assertEquals("**hello** world", edit.text)
        assertEquals(2, edit.start)
        assertEquals(7, edit.end)
    }

    @Test
    fun wrapNormalisesAReversedSelection() {
        val edit = EditorOps.wrap("abc", 3, 0, "*")
        assertEquals("*abc*", edit.text)
        assertEquals(1, edit.start)
        assertEquals(4, edit.end)
    }

    @Test
    fun wrapClampsOutOfRangeSelections() {
        val edit = EditorOps.wrap("ab", -5, 99, "`")
        assertEquals("`ab`", edit.text)
        assertEquals(1, edit.start)
        assertEquals(3, edit.end)
    }

    @Test
    fun lineStartAndEndFindTheEnclosingLine() {
        val text = "first\nsecond\nthird"
        assertEquals(0, EditorOps.lineStart(text, 0))
        assertEquals(0, EditorOps.lineStart(text, 4))
        assertEquals(6, EditorOps.lineStart(text, 8))
        assertEquals(5, EditorOps.lineEnd(text, 0))
        assertEquals(5, EditorOps.lineEnd(text, 3))
        assertEquals(12, EditorOps.lineEnd(text, 8))
        assertEquals(18, EditorOps.lineEnd(text, 16))
    }

    @Test
    fun toggleLinePrefixAddsThenRemoves() {
        val added = EditorOps.toggleLinePrefix("title", 0, 0, "# ")
        assertEquals("# title", added.text)

        val removed = EditorOps.toggleLinePrefix(added.text, 0, 0, "# ")
        assertEquals("title", removed.text)
    }

    @Test
    fun toggleLinePrefixAppliesToEveryTouchedLine() {
        val edit = EditorOps.toggleLinePrefix("a\nb\nc", 0, 3, "> ")
        assertEquals("> a\n> b\nc", edit.text)
    }

    @Test
    fun toggleLinePrefixLeavesBlankLinesAloneWhenRemoving() {
        val edit = EditorOps.toggleLinePrefix("# a\n\n# b", 0, 7, "# ")
        assertEquals("a\n\nb", edit.text)
    }

    @Test
    fun toggleLinePrefixAddsToAllLinesWhenOnlySomeAreMarked() {
        val edit = EditorOps.toggleLinePrefix("# a\nb", 0, 4, "# ")
        assertEquals("# # a\n# b", edit.text)
    }

    @Test
    fun toggleTaskCyclesBetweenStates() {
        val first = EditorOps.toggleTask("todo", 4, 4)
        assertEquals("- [ ] todo", first.text)

        val second = EditorOps.toggleTask(first.text, first.caret, first.caret)
        assertEquals("- [x] todo", second.text)

        val third = EditorOps.toggleTask(second.text, second.caret, second.caret)
        assertEquals("- [ ] todo", third.text)
    }

    @Test
    fun toggleTaskConvertsAPlainBullet() {
        assertEquals("- [ ] item", EditorOps.toggleTask("- item", 2, 2).text)
        assertEquals("- [ ] item", EditorOps.toggleTask("* item", 2, 2).text)
    }

    @Test
    fun toggleTaskKeepsIndentationAndOnlyTouchesTheCaretLine() {
        val text = "keep me\n  - [ ] nested"
        val edit = EditorOps.toggleTask(text, text.length, text.length)
        assertEquals("keep me\n  - [x] nested", edit.text)
    }

    @Test
    fun insertBlockPutsTheSnippetAtTheStartOfTheLine() {
        val edit = EditorOps.insertBlock("abc", 2, 2, "> ")
        assertEquals("> abc", edit.text)
        assertEquals(2, edit.caret)
    }

    @Test
    fun insertBlockWorksOnLaterLines() {
        val edit = EditorOps.insertBlock("one\ntwo", 6, 6, "> ")
        assertEquals("one\n> two", edit.text)
        assertEquals(6, edit.caret)
    }

    @Test
    fun indentAddsTwoSpacesThenRemovesThem() {
        val indented = EditorOps.indent("a\nb", 0, 3)
        assertEquals("  a\n  b", indented.text)

        val outdented = EditorOps.indent(indented.text, 0, indented.text.length)
        assertEquals("a\nb", outdented.text)
    }

    @Test
    fun statsCountWordsCharactersAndMinutes() {
        val stats = EditorOps.stats("one two")
        assertEquals(2, stats.words)
        assertEquals(7, stats.characters)
        assertEquals(1, stats.minutes)

        val empty = EditorOps.stats("")
        assertEquals(0, empty.words)
        assertEquals(0, empty.characters)
        assertEquals(0, empty.minutes)
    }

    @Test
    fun editsAreStableOnEmptyText() {
        val wrapped = EditorOps.wrap("", 0, 0, "*")
        assertEquals("**", wrapped.text)
        assertEquals(1, wrapped.caret)
        assertEquals("- [ ] ", EditorOps.toggleTask("", 0, 0).text)
        assertTrue(EditorOps.indent("", 0, 0).text.isEmpty())
    }

    @Test
    fun continueListAddsTheNextUncheckedTask() {
        val edit = EditorOps.continueList("- [x] done\n", 11)
        assertEquals("- [x] done\n- [ ] ", edit.text)
        assertEquals(edit.text.length, edit.caret)
    }

    @Test
    fun continueListKeepsBulletAndIndent() {
        val edit = EditorOps.continueList("  * item\n", 9)
        assertEquals("  * item\n  * ", edit.text)
    }

    @Test
    fun continueListIncrementsOrderedLists() {
        val edit = EditorOps.continueList("1. first\n", 9)
        assertEquals("1. first\n2. ", edit.text)
        val parenthesised = EditorOps.continueList("2) second\n", 11)
        assertEquals("2) second\n3) ", parenthesised.text)
    }

    @Test
    fun continueListCarriesQuotesForward() {
        val edit = EditorOps.continueList("> quoted\n", 9)
        assertEquals("> quoted\n> ", edit.text)
    }

    @Test
    fun continueListExitsOnAnEmptyItem() {
        val edit = EditorOps.continueList("  - [ ] \n", 9)
        assertEquals("", edit.text)
        assertEquals(0, edit.caret)
    }

    @Test
    fun continueListLeavesPlainTextAlone() {
        val text = "just prose\n"
        val edit = EditorOps.continueList(text, text.length)
        assertEquals(text, edit.text)
        assertEquals(text.length, edit.caret)
    }

    @Test
    fun replaceIsCaseInsensitiveByDefault() {
        val out = EditorOps.replacement("Cat cat CAT", "cat", "dog", matchCase = false)
        assertEquals("dog dog dog", out.text)
        assertEquals(3, out.count)
    }

    @Test
    fun replaceRespectsCaseWhenAsked() {
        val out = EditorOps.replacement("Cat cat", "cat", "dog", matchCase = true)
        assertEquals("Cat dog", out.text)
        assertEquals(1, out.count)
    }

    @Test
    fun replaceTreatsRegexMetacharactersLiterally() {
        val out = EditorOps.replacement("a.b a.b", ".", "!", matchCase = false)
        assertEquals("a!b a!b", out.text)
        assertEquals(2, out.count)
    }

    @Test
    fun replaceWithEmptyQueryIsANoOp() {
        val out = EditorOps.replacement("abc", "", "x", matchCase = false)
        assertEquals("abc", out.text)
        assertEquals(0, out.count)
    }

    @Test
    fun replaceKeepsOffsetsWhenCaseFoldingChangesLength() {
        val out = EditorOps.replacement("İx", "x", "y", matchCase = false)
        assertEquals("İy", out.text)
        assertEquals(1, out.count)
    }

    @Test
    fun replaceCanGrowTextAndKeepsTheRest() {
        val out = EditorOps.replacement("one and one", "one", "ninety-nine", matchCase = false)
        assertEquals("ninety-nine and ninety-nine", out.text)
        assertEquals(2, out.count)
    }

    @Test
    fun replaceCurrentMovesToTheNextOriginalMatch() {
        val step = EditorOps.replaceMatch("one two one", "one", "1", matchCase = false, index = 0)!!
        assertEquals("1 two one", step.text)
        assertEquals(0, step.nextIndex)
        assertEquals(6, EditorOps.matchRanges(step.text, "one", matchCase = false)[step.nextIndex].start)
    }

    @Test
    fun replaceCurrentSkipsMatchesIntroducedByTheReplacement() {
        val step = EditorOps.replaceMatch("cat cat", "cat", "cats", matchCase = false, index = 0)!!
        assertEquals("cats cat", step.text)
        assertEquals(1, step.nextIndex)
        assertEquals(5, EditorOps.matchRanges(step.text, "cat", matchCase = false)[step.nextIndex].start)
    }

    @Test
    fun replaceCurrentWrapsToTheFirstMatch() {
        val step = EditorOps.replaceMatch("one two one", "one", "1", matchCase = false, index = 1)!!
        assertEquals("one two 1", step.text)
        assertEquals(0, step.nextIndex)
    }

    @Test
    fun replaceCurrentKeepsTheNextOffsetAlignedWhenTheReplacementShrinks() {
        val step = EditorOps.replaceMatch("cat and cat", "cat", "", matchCase = false, index = 0)!!
        assertEquals(" and cat", step.text)
        assertEquals(0, step.nextIndex)
        assertEquals(5, EditorOps.matchRanges(step.text, "cat", matchCase = false)[0].start)
    }

    @Test
    fun replaceCurrentHasNoNextMatchAndRejectsMissingMatches() {
        assertEquals(
            -1,
            EditorOps.replaceMatch("cat", "cat", "dog", matchCase = false, index = 0)!!.nextIndex,
        )
        assertNull(EditorOps.replaceMatch("cat", "dog", "x", matchCase = false, index = 0))
        assertNull(EditorOps.replaceMatch("cat", "cat", "x", matchCase = false, index = 1))
    }

    @Test
    fun matchRangesAreNonOverlappingAndInDocumentOrder() {
        val ranges = EditorOps.matchRanges("aa aa", "aa", matchCase = false)
        assertEquals(listOf(0 to 2, 3 to 5), ranges.map { it.start to it.end })
    }

    @Test
    fun matchRangesRespectCaseAndTreatRegexCharactersLiterally() {
        assertEquals(
            listOf(4 to 7),
            EditorOps.matchRanges("Cat cat CAT", "cat", matchCase = true).map { it.start to it.end },
        )
        assertEquals(
            listOf(1 to 2, 4 to 5),
            EditorOps.matchRanges("a.b .", ".", matchCase = false).map { it.start to it.end },
        )
    }

    @Test
    fun matchRangesAreEmptyForAnEmptyQueryOrNoMatch() {
        assertTrue(EditorOps.matchRanges("abc", "", matchCase = false).isEmpty())
        assertTrue(EditorOps.matchRanges("abc", "z", matchCase = false).isEmpty())
    }

    @Test
    fun matchRangesKeepOffsetsWhenCaseFoldingChangesLength() {
        val ranges = EditorOps.matchRanges("İx", "x", matchCase = false)
        assertEquals(1, ranges.single().start)
        assertEquals(2, ranges.single().end)
    }

    @Test
    fun continueListExitsOnEmptyItemsForEveryStyle() {
        listOf(
            "- ",
            "* ",
            "1. ",
            "2) ",
            "> ",
            "- [ ] ",
            "  - ",
            "  > ",
        ).forEach { item ->
            val text = "$item\n"
            val edit = EditorOps.continueList(text, text.length)
            assertEquals("empty item should exit for '$item'", "", edit.text)
            assertEquals("caret should return to line start for '$item'", 0, edit.caret)
        }
    }

    @Test
    fun insertTemplateFillsBlankFields() {
        val inserted = EditorOps.insertTemplate(
            title = "",
            body = "",
            templateName = "Daily note",
            templateBody = "- [ ] One thing",
        )
        assertEquals("Daily note", inserted.title)
        assertEquals("- [ ] One thing", inserted.body)
    }

    @Test
    fun insertTemplateKeepsAnExistingTitle() {
        val inserted = EditorOps.insertTemplate(
            title = "My note",
            body = "existing",
            templateName = "Daily note",
            templateBody = "template",
        )
        assertEquals("My note", inserted.title)
        assertEquals("existing\n\ntemplate", inserted.body)
    }

    @Test
    fun insertTemplateSeparatesAnExistingBodyWithOneBlankLine() {
        val inserted = EditorOps.insertTemplate(
            title = "My note",
            body = "  existing  \n\n",
            templateName = "Daily note",
            templateBody = "template",
        )
        assertEquals("  existing\n\ntemplate", inserted.body)
    }

    @Test
    fun insertTemplateTreatsWhitespaceAsBlank() {
        val inserted = EditorOps.insertTemplate(
            title = "   ",
            body = " \n\t",
            templateName = "Meeting notes",
            templateBody = "## Attendees",
        )
        assertEquals("Meeting notes", inserted.title)
        assertEquals("## Attendees", inserted.body)
    }
}
