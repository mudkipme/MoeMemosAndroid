package me.mudkip.moememos.util

import org.junit.Assert.assertEquals
import org.junit.Test

class MarkdownTest {
    @Test
    fun extractCustomTags_excludesTagsInsideInlineLinkDestination() {
        val markdown = """
            [link](https://example.com/path#fragment)
            #realTag
        """.trimIndent()

        val tags = extractCustomTags(markdown)

        assertEquals(setOf("realTag"), tags)
    }

    @Test
    fun extractCustomTags_excludesTagsInsideAutolink() {
        val markdown = """
            <https://example.com/path#fragment>
            #realTag
        """.trimIndent()

        val tags = extractCustomTags(markdown)

        assertEquals(setOf("realTag"), tags)
    }

    @Test
    fun extractCustomTags_excludesTagsInsideCode() {
        val markdown = """
            `#inlineCode`
            
            ```kotlin
            val value = "#fencedCode"
            ```
            
            #realTag
        """.trimIndent()

        val tags = extractCustomTags(markdown)

        assertEquals(setOf("realTag"), tags)
    }

    @Test
    fun extractCustomTags_excludesTagsInsideReferenceLinks() {
        val markdown = """
            [docs][memos]
            [memos]: https://example.com/path#fragment
            #realTag
        """.trimIndent()

        val tags = extractCustomTags(markdown)

        assertEquals(setOf("realTag"), tags)
    }
}
