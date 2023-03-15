package me.mudkip.moememos.util

import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.parser.MarkdownParser

fun extractCustomTags(markdownText: String): Set<String> {
    val parsedTree = MarkdownParser(GFMFlavourDescriptor()).parse(MarkdownElementTypes.MARKDOWN_FILE, markdownText)
    val hashPattern = Regex("#([^\\s#]+)")
    val tags = HashSet<String>()

    fun isInsideCodeBlock(node: ASTNode): Boolean {
        var currentNode = node
        while (currentNode.type != MarkdownElementTypes.MARKDOWN_FILE) {
            if (currentNode.type == MarkdownElementTypes.CODE_FENCE || currentNode.type == MarkdownElementTypes.CODE_SPAN) {
                return true
            }
            currentNode = currentNode.parent!!
        }
        return false
    }

    fun isInsideLink(node: ASTNode): Boolean {
        var currentNode = node
        while (currentNode.type != MarkdownElementTypes.MARKDOWN_FILE) {
            if (currentNode.type == MarkdownElementTypes.LINK_DEFINITION || currentNode.type == MarkdownElementTypes.INLINE_LINK) {
                return true
            }
            currentNode = currentNode.parent!!
        }
        return false
    }

    hashPattern.findAll(markdownText).forEach { result ->
        val startPosition = result.range.first
        val node = parsedTree.findNodeAtPosition(startPosition)

        if (node != null && !isInsideCodeBlock(node) && !isInsideLink(node)) {
            tags.add(result.groupValues[1])
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
