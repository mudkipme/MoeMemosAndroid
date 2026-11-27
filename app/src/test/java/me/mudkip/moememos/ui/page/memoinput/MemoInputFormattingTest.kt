package me.mudkip.moememos.ui.page.memoinput

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MemoInputFormattingTest {
    // "|" marks the cursor position.
    private fun field(textWithCursor: String): TextFieldValue {
        val cursor = textWithCursor.indexOf('|')
        return TextFieldValue(textWithCursor.removeRange(cursor, cursor + 1), TextRange(cursor))
    }

    private fun render(value: TextFieldValue?): String? {
        if (value == null) return null
        assertEquals(value.selection.min, value.selection.max)
        return value.text.substring(0, value.selection.min) + "|" + value.text.substring(value.selection.min)
    }

    private fun enter(textWithCursor: String) = render(handleEnterInText(field(textWithCursor)))

    @Test
    fun continuesNumberedList() {
        assertEquals("1. Bread\n2. |", enter("1. Bread|"))
    }

    @Test
    fun continuesNumberedListAfterPreviousItems() {
        assertEquals("1. Bread\n2. Milk\n3. |", enter("1. Bread\n2. Milk|"))
    }

    @Test
    fun goesFromNineToTen() {
        assertEquals("9. Milk\n10. |", enter("9. Milk|"))
    }

    @Test
    fun keepsParenthesisDelimiter() {
        assertEquals("1) Bread\n2) |", enter("1) Bread|"))
    }

    @Test
    fun keepsIndentationOfNestedItems() {
        assertEquals("1. Fruit\n   1. Apple\n   2. |", enter("1. Fruit\n   1. Apple|"))
    }

    @Test
    fun splitsItemWhenCursorIsInTheMiddle() {
        assertEquals("1. Bre\n2. |ad", enter("1. Bre|ad"))
    }

    @Test
    fun doesNotContinueEmptyItem() {
        assertNull(enter("1. Bread\n2. |"))
    }

    @Test
    fun doesNotContinueWhenCursorIsInsidePrefix() {
        assertNull(enter("1|. Bread"))
    }

    @Test
    fun ignoresTextThatIsNotAListItem() {
        assertNull(enter("Plain text|"))
        assertNull(enter("1.5 kg of flour|"))
    }

    @Test
    fun stillContinuesBulletList() {
        assertEquals("- Bread\n- |", enter("- Bread|"))
    }
}
