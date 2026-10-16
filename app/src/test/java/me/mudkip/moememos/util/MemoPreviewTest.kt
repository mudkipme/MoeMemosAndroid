package me.mudkip.moememos.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MemoPreviewTest {
    @Test
    fun shortContent_isNotTruncated() {
        val (text, truncated) = extractPreviewContent("Hello world")
        assertEquals("Hello world", text)
        assertFalse(truncated)
    }

    @Test
    fun latinContent_spendsOneUnitPerCharacter() {
        val (text, truncated) = extractPreviewContent("a".repeat(1000))
        assertTrue(truncated)
        assertEquals("a".repeat(500) + "…", text)
    }

    @Test
    fun fullWidthContent_spendsTwoUnitsPerCharacter() {
        val (text, truncated) = extractPreviewContent("中".repeat(1000))
        assertTrue(truncated)
        assertEquals("中".repeat(250) + "…", text)
    }

    @Test
    fun mixedContent_countsEachCharacterByItsWidth() {
        val (text, truncated) = extractPreviewContent("a".repeat(250) + "中".repeat(200))
        assertTrue(truncated)
        assertEquals("a".repeat(250) + "中".repeat(125) + "…", text)
    }

    @Test
    fun surrogatePair_isNotSplit() {
        val (text, truncated) = extractPreviewContent("a".repeat(499) + "\uD83D\uDE00" + "b")
        assertTrue(truncated)
        val body = text.removeSuffix("…")
        assertEquals(499 + 2, body.length)
        assertEquals(500, body.codePointCount(0, body.length))
    }

    @Test
    fun previewWidth_countsFullWidthCharactersAsTwo() {
        assertEquals(2, previewWidth("ab"))
        assertEquals(4, previewWidth("a中b"))
        assertEquals(4, previewWidth("中文"))
        assertEquals(6, previewWidth("，。！"))
        assertEquals(1, previewWidth("\uD83D\uDE00"))
    }
}
