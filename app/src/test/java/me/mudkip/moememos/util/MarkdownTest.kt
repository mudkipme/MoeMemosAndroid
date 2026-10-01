package me.mudkip.moememos.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownTest {
    @Test
    fun hasCustomTag_matchesExactTagsAndDescendants() {
        assertTrue(hasCustomTag("A note with #foo and #other", "foo"))
        assertTrue(hasCustomTag("#foo/bar", "foo"))
        assertTrue(hasCustomTag("#foo/bar/baz", "foo"))
        assertTrue(hasCustomTag("#foo/bar/baz", "foo/bar"))
        assertTrue(hasCustomTag("#foo/bar", "foo/bar"))
        assertTrue(hasCustomTag("#旅行/東京", "旅行"))
    }

    @Test
    fun hasCustomTag_rejectsSimilarNamesAndUnrelatedPaths() {
        assertFalse(hasCustomTag("#foobar", "foo"))
        assertFalse(hasCustomTag("#foobar/baz", "foo"))
        assertFalse(hasCustomTag("#foo/barista", "foo/bar"))
        assertFalse(hasCustomTag("#other/foo", "foo"))
        assertFalse(hasCustomTag("#foo", "foo/bar"))
        assertFalse(hasCustomTag("No tags here", "foo"))
    }

    @Test
    fun hasCustomTag_preservesCaseSensitiveMatching() {
        assertTrue(hasCustomTag("#Foo/bar", "Foo"))
        assertFalse(hasCustomTag("#Foo", "foo"))
        assertFalse(hasCustomTag("#Foo/bar", "foo"))
    }

    @Test
    fun hasCustomTag_excludesCode() {
        val markdown = """
            `#foo`

            ```kotlin
            val value = "#foo/bar"
            ```

                #foo/baz
        """.trimIndent()

        assertFalse(hasCustomTag(markdown, "foo"))
        assertTrue(hasCustomTag("$markdown\n\n#foo", "foo"))
    }

    @Test
    fun hasCustomTag_excludesLinksAndImages() {
        val markdown = """
            [#foo](https://example.com/#foo/bar)
            <https://example.com/#foo>
            https://example.com/#foo/baz
            ![#foo](https://example.com/image.png#foo)

            [docs][reference]

            [reference]: https://example.com/#foo
        """.trimIndent()

        assertFalse(hasCustomTag(markdown, "foo"))
        assertTrue(hasCustomTag("$markdown\n\n#foo/bar", "foo"))
    }

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
