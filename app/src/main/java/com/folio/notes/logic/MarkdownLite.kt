package com.folio.notes.logic

/** Inline emphasis carried by a run of text. */
enum class MarkStyle { NORMAL, BOLD, ITALIC, BOLD_ITALIC, CODE, STRIKE }

data class Span(val text: String, val style: MarkStyle = MarkStyle.NORMAL)

/** A rendered block of markdown-lite. */
sealed interface Block {
    data class Heading(val level: Int, val spans: List<Span>) : Block
    data class Paragraph(val spans: List<Span>) : Block
    data class Bullet(
        val spans: List<Span>,
        val indent: Int,
        val checked: Boolean?,
        val line: Int,
    ) : Block
    data class Quote(val spans: List<Span>) : Block
    data class Code(val code: String) : Block
    data object Rule : Block
}

/**
 * A deliberately small markdown dialect: headings, bullets, task lists, quotes,
 * fenced code, rules, and inline bold / italic / strike / code.
 * It is pure Kotlin so the parser can be unit tested on the JVM.
 */
object MarkdownLite {

    fun parse(markdown: String): List<Block> {
        val lines = markdown.split('\n')
        val blocks = mutableListOf<Block>()
        var i = 0
        val paragraph = StringBuilder()

        fun flushParagraph() {
            if (paragraph.isNotEmpty()) {
                blocks += Block.Paragraph(inline(paragraph.toString()))
                paragraph.setLength(0)
            }
        }

        while (i < lines.size) {
            val raw = lines[i]
            val line = raw.trimEnd()
            val trimmed = line.trimStart()

            if (trimmed.startsWith("```")) {
                flushParagraph()
                val code = StringBuilder()
                i++
                while (i < lines.size && !lines[i].trimStart().startsWith("```")) {
                    if (code.isNotEmpty()) code.append('\n')
                    code.append(lines[i])
                    i++
                }
                blocks += Block.Code(code.toString())
                i++
                continue
            }

            when {
                trimmed.isEmpty() -> {
                    flushParagraph()
                    i++
                }

                trimmed.length >= 3 && trimmed.all { it == '-' || it == '*' || it == '_' } &&
                    trimmed.toSet().size == 1 -> {
                    flushParagraph()
                    blocks += Block.Rule
                    i++
                }

                headingLevel(trimmed) > 0 -> {
                    flushParagraph()
                    val level = headingLevel(trimmed)
                    val text = trimmed.dropWhile { it == '#' }.trim()
                    blocks += Block.Heading(level, inline(text))
                    i++
                }

                taskMatch(trimmed) != null -> {
                    flushParagraph()
                    val match = taskMatch(trimmed)!!
                    val indent = indentOf(line)
                    blocks += Block.Bullet(
                        spans = inline(match.text),
                        indent = indent,
                        checked = match.checked,
                        line = i,
                    )
                    i++
                }

                isBullet(trimmed) -> {
                    flushParagraph()
                    val indent = indentOf(line)
                    val text = trimmed.drop(1).trimStart()
                    blocks += Block.Bullet(inline(text), indent, null, i)
                    i++
                }

                trimmed.startsWith(">") -> {
                    flushParagraph()
                    blocks += Block.Quote(inline(trimmed.drop(1).trimStart()))
                    i++
                }

                else -> {
                    if (paragraph.isNotEmpty()) paragraph.append(' ')
                    paragraph.append(trimmed)
                    i++
                }
            }
        }
        flushParagraph()
        return blocks
    }

    private fun headingLevel(trimmed: String): Int {
        var level = 0
        while (level < trimmed.length && level < 6 && trimmed[level] == '#') level++
        if (level == 0) return 0
        if (level == trimmed.length || trimmed[level] != ' ') return 0
        return level
    }

    private fun isBullet(trimmed: String) =
        trimmed.length > 1 && (trimmed[0] == '-' || trimmed[0] == '*') && trimmed[1] == ' '

    private data class Task(val checked: Boolean, val text: String)

    private fun taskMatch(trimmed: String): Task? {
        if (trimmed.length < 5) return null
        if (trimmed[0] != '-' && trimmed[0] != '*') return null
        if (trimmed[1] != ' ') return null
        if (trimmed[2] != '[' || trimmed[4] != ']') return null
        val mark = trimmed[3]
        if (mark != ' ' && mark != 'x' && mark != 'X') return null
        val rest = trimmed.drop(5)
        if (rest.isNotEmpty() && rest[0] != ' ') return null
        return Task(mark != ' ', rest.trimStart())
    }

    private fun indentOf(line: String): Int = (line.takeWhile { it == ' ' }.length / 2).coerceAtMost(4)

    fun inline(text: String): List<Span> {
        val out = mutableListOf<Span>()
        val buffer = StringBuilder()
        var i = 0

        fun flush() {
            if (buffer.isNotEmpty()) {
                out += Span(buffer.toString())
                buffer.setLength(0)
            }
        }

        while (i < text.length) {
            val c = text[i]
            when {
                c == '\\' && i + 1 < text.length -> {
                    buffer.append(text[i + 1])
                    i += 2
                }

                (text.startsWith("**", i) || text.startsWith("__", i)) -> {
                    val marker = text.substring(i, i + 2)
                    val end = text.indexOf(marker, i + 2)
                    if (end > i + 2) {
                        flush()
                        out += Span(text.substring(i + 2, end), MarkStyle.BOLD)
                        i = end + 2
                    } else {
                        buffer.append(c)
                        i++
                    }
                }

                c == '`' -> {
                    val end = text.indexOf('`', i + 1)
                    if (end > i + 1) {
                        flush()
                        out += Span(text.substring(i + 1, end), MarkStyle.CODE)
                        i = end + 1
                    } else {
                        buffer.append(c)
                        i++
                    }
                }

                text.startsWith("~~", i) -> {
                    val end = text.indexOf("~~", i + 2)
                    if (end > i + 2) {
                        flush()
                        out += Span(text.substring(i + 2, end), MarkStyle.STRIKE)
                        i = end + 2
                    } else {
                        buffer.append(c)
                        i++
                    }
                }

                c == '*' || c == '_' -> {
                    val end = text.indexOf(c, i + 1)
                    if (end > i + 1) {
                        flush()
                        out += Span(text.substring(i + 1, end), MarkStyle.ITALIC)
                        i = end + 1
                    } else {
                        buffer.append(c)
                        i++
                    }
                }

                else -> {
                    buffer.append(c)
                    i++
                }
            }
        }
        flush()
        return out
    }

    /** Flattens markdown to readable text: used for previews, search and titles. */
    fun plainText(markdown: String): String =
        parse(markdown).joinToString("\n") { block ->
            when (block) {
                is Block.Heading -> block.spans.joinToString("") { it.text }
                is Block.Paragraph -> block.spans.joinToString("") { it.text }
                is Block.Bullet -> {
                    // The task glyphs come from the symbol fallback font, whose ink reaches the
                    // edge of its advance width. A non-breaking space keeps a visible gap so the
                    // preview never reads as "☐Espresso".
                    val box = when (block.checked) {
                        true -> "☑\u00A0 "
                        false -> "☐\u00A0 "
                        null -> "•\u00A0 "
                    }
                    box + block.spans.joinToString("") { it.text }
                }
                is Block.Quote -> block.spans.joinToString("") { it.text }
                is Block.Code -> block.code
                Block.Rule -> ""
            }
        }

    fun firstLine(markdown: String): String =
        plainText(markdown)
            .lineSequence()
            .map { it.trim() }
            .firstOrNull { it.isNotEmpty() }
            .orEmpty()

    /** Flips the checkbox on the given source line, returning the new markdown. */
    fun toggleCheckbox(markdown: String, line: Int): String {
        val lines = markdown.split('\n').toMutableList()
        if (line !in lines.indices) return markdown
        val target = lines[line]
        val indent = target.takeWhile { it == ' ' }
        val body = target.drop(indent.length)
        val parser = Regex("^([-*]) \\[([ xX])](.*)$")
        val match = parser.find(body) ?: return markdown
        val mark = match.groupValues[2]
        val rest = match.groupValues[3]
        val next = if (mark == " ") "x" else " "
        val bullet = match.groupValues[1]
        lines[line] = "$indent$bullet [$next]$rest"
        return lines.joinToString("\n")
    }
}
