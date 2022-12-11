package me.mudkip.moememos.ext

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.*
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.getTextInNode
import org.intellij.markdown.flavours.gfm.GFMTokenTypes

@OptIn(ExperimentalTextApi::class)
fun AnnotatedString.Builder.appendMarkdown(
    markdownText: String,
    node: ASTNode,
    depth: Int = 0,
    linkColor: Color
): AnnotatedString.Builder {
    when (node.type) {
        MarkdownElementTypes.MARKDOWN_FILE, MarkdownElementTypes.PARAGRAPH -> {
            node.children.forEach { childNode ->
                appendMarkdown(
                    markdownText = markdownText,
                    node = childNode,
                    depth = depth + 1,
                    linkColor = linkColor
                )
            }
        }

        MarkdownElementTypes.INLINE_LINK -> {
            val linkDestination =
                node.children.findLast { it.type == MarkdownElementTypes.LINK_DESTINATION }
                    ?: return this
            val linkText = node.children.find { it.type == MarkdownElementTypes.LINK_TEXT }!!
                .children[1]

            withAnnotation(UrlAnnotation(linkDestination.getTextInNode(markdownText).toString())) {
                withStyle(SpanStyle(linkColor)) {
                    appendMarkdown(
                        markdownText = markdownText,
                        node = linkText,
                        depth = depth + 1,
                        linkColor = linkColor
                    )
                }
            }
        }

        GFMTokenTypes.GFM_AUTOLINK -> {
            val linkDestination = node.getTextInNode(markdownText).toString()
            withAnnotation(UrlAnnotation(linkDestination)) {
                withStyle(SpanStyle(linkColor)) {
                    append(linkDestination)
                }
            }
        }

        else -> {
            append(node.getTextInNode(markdownText).toString())
        }
    }
    return this
}