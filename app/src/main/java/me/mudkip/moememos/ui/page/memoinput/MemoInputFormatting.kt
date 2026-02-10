package me.mudkip.moememos.ui.page.memoinput

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import me.mudkip.moememos.data.constant.LIST_ITEM_SYMBOL_LIST

internal enum class MarkdownFormat(
    val label: String,
    val prefix: String,
    val suffix: String = "",
    val isLinePrefix: Boolean = false
) {
    H1("H1", "# ", isLinePrefix = true),
    H2("H2", "## ", isLinePrefix = true),
    H3("H3", "### ", isLinePrefix = true),
    BOLD("B", "**", "**"),
    ITALIC("I", "*", "*"),
    STRIKETHROUGH("S", "~~", "~~"),
    BULLET("•", "- ", isLinePrefix = true),
    NUMBERED("1.", "1. ", isLinePrefix = true)
}

internal fun toggleTodoItemInText(text: TextFieldValue): TextFieldValue {
    val contentBefore = text.text.substring(0, text.selection.min)
    val lastLineBreak = contentBefore.indexOfLast { it == '\n' }
    val nextLineBreak = text.text.indexOf('\n', lastLineBreak + 1)
    val currentLine = text.text.substring(
        lastLineBreak + 1,
        if (nextLineBreak == -1) text.text.length else nextLineBreak
    )
    val contentBeforeCurrentLine = contentBefore.take(lastLineBreak + 1)
    val contentAfterCurrentLine = if (nextLineBreak == -1) "" else text.text.substring(nextLineBreak)

    for (prefix in LIST_ITEM_SYMBOL_LIST) {
        if (!currentLine.startsWith(prefix)) {
            continue
        }

        if (prefix == "- [ ] ") {
            return text.copy(contentBeforeCurrentLine + "- [x] " + currentLine.substring(prefix.length) + contentAfterCurrentLine)
        }

        val offset = "- [ ] ".length - prefix.length
        return text.copy(
            contentBeforeCurrentLine + "- [ ] " + currentLine.substring(prefix.length) + contentAfterCurrentLine,
            TextRange(text.selection.start + offset, text.selection.end + offset)
        )
    }

    return text.copy(
        "$contentBeforeCurrentLine- [ ] $currentLine$contentAfterCurrentLine",
        TextRange(text.selection.start + "- [ ] ".length, text.selection.end + "- [ ] ".length)
    )
}

internal fun handleEnterInText(text: TextFieldValue): TextFieldValue? {
    val contentBefore = text.text.substring(0, text.selection.min)
    val lastLineBreak = contentBefore.indexOfLast { it == '\n' }
    val nextLineBreak = text.text.indexOf('\n', lastLineBreak + 1)
    val currentLine = text.text.substring(
        lastLineBreak + 1,
        if (nextLineBreak == -1) text.text.length else nextLineBreak
    )

    for (prefix in LIST_ITEM_SYMBOL_LIST) {
        if (!currentLine.startsWith(prefix)) {
            continue
        }

        if (currentLine.length <= prefix.length || text.selection.min - lastLineBreak <= prefix.length) {
            break
        }

        return text.copy(
            text.text.substring(0, text.selection.min) + "\n" + prefix + text.text.substring(text.selection.max),
            TextRange(text.selection.min + 1 + prefix.length, text.selection.min + 1 + prefix.length)
        )
    }

    return null
}

internal fun applyMarkdownFormatToText(text: TextFieldValue, format: MarkdownFormat): TextFieldValue {
    if (format.isLinePrefix) {
        val contentBefore = text.text.substring(0, text.selection.min)
        val lastLineBreak = contentBefore.indexOfLast { it == '\n' }
        val nextLineBreak = text.text.indexOf('\n', lastLineBreak + 1)
        val currentLine = text.text.substring(
            lastLineBreak + 1,
            if (nextLineBreak == -1) text.text.length else nextLineBreak
        )
        val contentBeforeCurrentLine = contentBefore.take(lastLineBreak + 1)
        val contentAfterCurrentLine = if (nextLineBreak == -1) "" else text.text.substring(nextLineBreak)
        val linePrefixCandidates = (
            MarkdownFormat.entries
                .filter { it.isLinePrefix }
                .map { it.prefix } + LIST_ITEM_SYMBOL_LIST
            )
            .distinct()
            .sortedByDescending { it.length }
        val existingPrefix = linePrefixCandidates.firstOrNull { currentLine.startsWith(it) }
        val lineWithoutPrefix = if (existingPrefix != null) {
            currentLine.substring(existingPrefix.length)
        } else {
            currentLine
        }
        val nextPrefix = if (existingPrefix == format.prefix) "" else format.prefix
        val newLine = nextPrefix + lineWithoutPrefix
        val newText = contentBeforeCurrentLine + newLine + contentAfterCurrentLine
        val offset = nextPrefix.length - (existingPrefix?.length ?: 0)

        return text.copy(
            newText,
            TextRange(
                (text.selection.start + offset).coerceIn(0, newText.length),
                (text.selection.end + offset).coerceIn(0, newText.length)
            )
        )
    }

    if (text.selection.min != text.selection.max) {
        val selectedText = text.text.substring(text.selection.min, text.selection.max)
        val newText = text.text.substring(0, text.selection.min) +
            format.prefix + selectedText + format.suffix +
            text.text.substring(text.selection.max)
        val newPosition = text.selection.max + format.prefix.length + format.suffix.length
        return text.copy(
            newText,
            TextRange(newPosition, newPosition)
        )
    }

    val newText = text.text.substring(0, text.selection.min) +
        format.prefix + format.suffix +
        text.text.substring(text.selection.min)
    val cursorPosition = text.selection.min + format.prefix.length
    return text.copy(
        newText,
        TextRange(cursorPosition, cursorPosition)
    )
}

internal fun replaceSelection(text: TextFieldValue, replacement: String): TextFieldValue {
    val start = text.selection.min
    val end = text.selection.max
    val updated = text.text.replaceRange(start, end, replacement)
    return text.copy(updated, TextRange(start + replacement.length))
}
