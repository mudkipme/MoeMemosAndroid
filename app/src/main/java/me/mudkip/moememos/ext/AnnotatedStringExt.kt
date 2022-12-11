package me.mudkip.moememos.ext

import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.getTextInNode
import org.intellij.markdown.flavours.gfm.GFMTokenTypes
import java.util.*

@OptIn(ExperimentalTextApi::class)
fun AnnotatedString.Builder.appendMarkdown(
    markdownText: String,
    node: ASTNode,
    depth: Int = 0,
    linkColor: Color,
    onImage: (id: String, link: String) -> Unit,
    maxWidth: Float
): AnnotatedString.Builder {
    when (node.type) {
        MarkdownElementTypes.MARKDOWN_FILE, MarkdownElementTypes.PARAGRAPH -> {
            node.children.forEach { childNode ->
                appendMarkdown(
                    markdownText = markdownText,
                    node = childNode,
                    depth = depth + 1,
                    linkColor = linkColor,
                    onImage = onImage,
                    maxWidth = maxWidth
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
                        linkColor = linkColor,
                        onImage = onImage,
                        maxWidth = maxWidth
                    )
                }
            }
        }

        MarkdownElementTypes.AUTOLINK, GFMTokenTypes.GFM_AUTOLINK -> {
            val linkDestination = node.getTextInNode(markdownText).toString()
            withAnnotation(UrlAnnotation(linkDestination)) {
                withStyle(SpanStyle(linkColor)) {
                    append(linkDestination)
                }
            }
        }

        MarkdownElementTypes.EMPH -> {
            withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                node.children.filter { it.type != MarkdownTokenTypes.EMPH }.forEach { childNode ->
                    appendMarkdown(
                        markdownText = markdownText,
                        node = childNode,
                        depth = depth + 1,
                        linkColor = linkColor,
                        onImage = onImage,
                        maxWidth = maxWidth
                    )
                }
            }
        }

        MarkdownElementTypes.STRONG -> {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                node.children.filter { it.type != MarkdownTokenTypes.EMPH }.forEach { childNode ->
                    appendMarkdown(
                        markdownText = markdownText,
                        node = childNode,
                        depth = depth + 1,
                        linkColor = linkColor,
                        onImage = onImage,
                        maxWidth = maxWidth
                    )
                }
            }
        }

        MarkdownElementTypes.CODE_SPAN -> {
            withStyle(SpanStyle(fontFamily = FontFamily.Monospace)) {
                node.children.filter { it.type != MarkdownTokenTypes.BACKTICK }.forEach { childNode ->
                    appendMarkdown(
                        markdownText = markdownText,
                        node = childNode,
                        depth = depth + 1,
                        linkColor = linkColor,
                        onImage = onImage,
                        maxWidth = maxWidth
                    )
                }
            }
        }

        MarkdownElementTypes.CODE_FENCE -> {
            withStyle(SpanStyle(fontFamily = FontFamily.Monospace)) {
                node.children.filter {
                    it.type != MarkdownTokenTypes.CODE_FENCE_START
                        && it.type != MarkdownTokenTypes.CODE_FENCE_END
                        && it.type != MarkdownTokenTypes.FENCE_LANG
                }.drop(1).dropLast(1).forEach { childNode ->
                    appendMarkdown(
                        markdownText = markdownText,
                        node = childNode,
                        depth = depth + 1,
                        linkColor = linkColor,
                        onImage = onImage,
                        maxWidth = maxWidth
                    )
                }
            }
        }

        MarkdownElementTypes.IMAGE -> {
            val linkNode = node.children.findLast { it.type == MarkdownElementTypes.INLINE_LINK } ?: return this
            val imageUrlNode =
                linkNode.children.findLast { it.type == MarkdownElementTypes.LINK_DESTINATION }
                    ?: return this
            val imageUrl = imageUrlNode.getTextInNode(markdownText).toString()
            val id = UUID.randomUUID().toString()

            onImage(id, imageUrl)
            withStyle(ParagraphStyle(lineHeight = (maxWidth * 9f / 16f).sp)) {
                appendInlineContent(id, imageUrl)
            }
        }

        else -> {
            append(node.getTextInNode(markdownText).toString())
        }
    }
    return this
}