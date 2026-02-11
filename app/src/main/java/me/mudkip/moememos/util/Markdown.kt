package me.mudkip.moememos.util

import org.intellij.markdown.IElementType
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.flavours.gfm.GFMTokenTypes
import org.intellij.markdown.parser.MarkdownParser

private val customTagPattern = Regex("#([^\\s#]+)")
private val customTagExcludedTypes = setOf(
    MarkdownElementTypes.CODE_BLOCK,
    MarkdownElementTypes.CODE_FENCE,
    MarkdownElementTypes.CODE_SPAN,
    MarkdownTokenTypes.CODE_LINE,
    MarkdownTokenTypes.CODE_FENCE_CONTENT,
    MarkdownElementTypes.LINK_DEFINITION,
    MarkdownElementTypes.INLINE_LINK,
    MarkdownElementTypes.FULL_REFERENCE_LINK,
    MarkdownElementTypes.SHORT_REFERENCE_LINK,
    MarkdownElementTypes.LINK_DESTINATION,
    MarkdownElementTypes.LINK_TEXT,
    MarkdownElementTypes.LINK_LABEL,
    MarkdownElementTypes.LINK_TITLE,
    MarkdownElementTypes.AUTOLINK,
    MarkdownElementTypes.IMAGE,
    MarkdownTokenTypes.URL,
    MarkdownTokenTypes.AUTOLINK,
    MarkdownTokenTypes.EMAIL_AUTOLINK,
    GFMTokenTypes.GFM_AUTOLINK
)

internal fun findCustomTagMatches(text: String): Sequence<MatchResult> {
    return customTagPattern.findAll(text)
}

internal fun getCustomTagName(matchResult: MatchResult): String {
    return matchResult.groupValues[1]
}

internal fun isCustomTagSupportedNode(node: ASTNode): Boolean {
    return !hasAncestorOfType(node, customTagExcludedTypes)
}

internal fun hasAncestorOfType(node: ASTNode, types: Set<IElementType>): Boolean {
    var currentNode: ASTNode? = node
    while (currentNode != null) {
        if (currentNode.type in types) {
            return true
        }
        currentNode = currentNode.parent
    }
    return false
}

fun extractCustomTags(markdownText: String): Set<String> {
    val parsedTree = MarkdownParser(GFMFlavourDescriptor()).parse(MarkdownElementTypes.MARKDOWN_FILE, markdownText)
    val tags = HashSet<String>()

    findCustomTagMatches(markdownText).forEach { result ->
        val startPosition = result.range.first
        val node = parsedTree.findNodeAtPosition(startPosition)

        if (node != null && isCustomTagSupportedNode(node)) {
            tags.add(getCustomTagName(result))
        }
    }

    return tags
}

fun ASTNode.findNodeAtPosition(position: Int): ASTNode? {
    if (position in startOffset until endOffset) {
        for (child in children) {
            val childNode = child.findNodeAtPosition(position)
            if (childNode != null) {
                return childNode
            }
        }
        return this
    }
    return null
}
