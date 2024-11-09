package me.mudkip.moememos.ext

import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.LinkInteractionListener
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.getTextInNode
import org.intellij.markdown.flavours.gfm.GFMElementTypes
import org.intellij.markdown.flavours.gfm.GFMTokenTypes
import java.util.UUID

fun AnnotatedString.Builder.appendMarkdown(
    markdownText: String,
    node: ASTNode,
    depth: Int = 0,
    linkColor: Color,
    linkInteractionListener: LinkInteractionListener?,
    onImage: (id: String, link: String) -> Unit,
    onCheckbox: (id: String, startOffset: Int, endOffset: Int) -> Unit,
    maxWidth: Float,
    bulletColor: Color,
    headlineLarge: TextStyle,
    headlineMedium: TextStyle,
    headlineSmall: TextStyle,
): AnnotatedString.Builder {
    when (node.type) {
        MarkdownElementTypes.MARKDOWN_FILE, MarkdownElementTypes.PARAGRAPH, MarkdownElementTypes.UNORDERED_LIST, MarkdownElementTypes.ORDERED_LIST -> {
            // Remove EOL after a headline
            val headlineTypes = listOf(
                MarkdownElementTypes.ATX_1,
                MarkdownElementTypes.ATX_2,
                MarkdownElementTypes.ATX_3,
                MarkdownElementTypes.SETEXT_1,
                MarkdownElementTypes.SETEXT_2
            )
            val children = node.children.filterIndexed { index, childNode ->
                !(childNode.type == MarkdownTokenTypes.EOL && index > 0 && headlineTypes.contains(node.children[index - 1].type))
            }

            children.forEach { childNode ->
                appendMarkdown(
                    markdownText = markdownText,
                    node = childNode,
                    depth = depth + 1,
                    linkColor = linkColor,
                    linkInteractionListener = linkInteractionListener,
                    onImage = onImage,
                    onCheckbox = onCheckbox,
                    maxWidth = maxWidth,
                    bulletColor = bulletColor,
                    headlineLarge = headlineLarge,
                    headlineMedium = headlineMedium,
                    headlineSmall = headlineSmall
                )
            }
        }

        MarkdownElementTypes.INLINE_LINK -> {
            val linkDestination =
                node.children.findLast { it.type == MarkdownElementTypes.LINK_DESTINATION }
                    ?: return this
            val linkText = node.children.find { it.type == MarkdownElementTypes.LINK_TEXT }?.children

            withLink(
                LinkAnnotation.Url(
                    url = linkDestination.getTextInNode(markdownText).toString(),
                    linkInteractionListener = linkInteractionListener
                )
            ) {
                withStyle(SpanStyle(linkColor)) {
                    linkText?.filterIndexed { index, _ -> index != 0 && index != linkText.size - 1 }
                        ?.forEach { childNode ->
                            appendMarkdown(
                                markdownText = markdownText,
                                node = childNode,
                                depth = depth + 1,
                                linkColor = linkColor,
                                linkInteractionListener = linkInteractionListener,
                                onImage = onImage,
                                onCheckbox = onCheckbox,
                                maxWidth = maxWidth,
                                bulletColor = bulletColor,
                                headlineLarge = headlineLarge,
                                headlineMedium = headlineMedium,
                                headlineSmall = headlineSmall
                            )
                        } ?: Unit
                }
            }
        }

        MarkdownElementTypes.AUTOLINK, GFMTokenTypes.GFM_AUTOLINK -> {
            val linkDestination = node.getTextInNode(markdownText).toString()
            withLink(
                LinkAnnotation.Url(
                    url = linkDestination,
                    linkInteractionListener = linkInteractionListener
                )
            ) {
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
                        linkInteractionListener = linkInteractionListener,
                        onImage = onImage,
                        onCheckbox = onCheckbox,
                        maxWidth = maxWidth,
                        bulletColor = bulletColor,
                        headlineLarge = headlineLarge,
                        headlineMedium = headlineMedium,
                        headlineSmall = headlineSmall
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
                        linkInteractionListener = linkInteractionListener,
                        onImage = onImage,
                        onCheckbox = onCheckbox,
                        maxWidth = maxWidth,
                        bulletColor = bulletColor,
                        headlineLarge = headlineLarge,
                        headlineMedium = headlineMedium,
                        headlineSmall = headlineSmall
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
                        linkInteractionListener = linkInteractionListener,
                        onImage = onImage,
                        onCheckbox = onCheckbox,
                        maxWidth = maxWidth,
                        bulletColor = bulletColor,
                        headlineLarge = headlineLarge,
                        headlineMedium = headlineMedium,
                        headlineSmall = headlineSmall
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
                        linkInteractionListener = linkInteractionListener,
                        onImage = onImage,
                        onCheckbox = onCheckbox,
                        maxWidth = maxWidth,
                        bulletColor = bulletColor,
                        headlineLarge = headlineLarge,
                        headlineMedium = headlineMedium,
                        headlineSmall = headlineSmall
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

        MarkdownElementTypes.ATX_1,
        MarkdownElementTypes.SETEXT_1,
        MarkdownElementTypes.ATX_2,
        MarkdownElementTypes.SETEXT_2,
        MarkdownElementTypes.ATX_3 -> {
            val content = node.children.find { it.type == MarkdownTokenTypes.ATX_CONTENT || it.type == MarkdownTokenTypes.SETEXT_CONTENT }

            val textStyle = when (node.type) {
                MarkdownElementTypes.ATX_1, MarkdownElementTypes.SETEXT_1 -> headlineLarge
                MarkdownElementTypes.ATX_2, MarkdownElementTypes.SETEXT_2 -> headlineMedium
                else -> headlineSmall
            }

            if (content != null) {
                var children = content.children
                if (children.firstOrNull()?.type == MarkdownTokenTypes.WHITE_SPACE) {
                    children = children.drop(1)
                }

                withStyle(textStyle.toParagraphStyle()) {
                    withStyle(textStyle.toSpanStyle()) {
                        children.forEach {
                            appendMarkdown(
                                markdownText = markdownText,
                                node = it,
                                depth = depth + 1,
                                linkColor = linkColor,
                                linkInteractionListener = linkInteractionListener,
                                onImage = onImage,
                                onCheckbox = onCheckbox,
                                maxWidth = maxWidth,
                                bulletColor = bulletColor,
                                headlineLarge = headlineLarge,
                                headlineMedium = headlineMedium,
                                headlineSmall = headlineSmall
                            )
                        }
                    }
                }
            }
        }

        MarkdownElementTypes.LIST_ITEM -> {
            var children = node.children
            if (node.children.size >= 2 && node.children[1].type == GFMTokenTypes.CHECK_BOX) {
                val id = UUID.randomUUID().toString()
                onCheckbox(id, node.children[1].startOffset, node.children[1].endOffset)
                appendInlineContent(id, node.children[1].getTextInNode(markdownText).toString())
                append(' ')
                children = children.drop(2)
            } else {
                withStyle(SpanStyle(color = bulletColor, fontWeight = FontWeight.Bold)) {
                    append(children[0].getTextInNode(markdownText).toString())
                }
                children = children.drop(1)
            }
            children.forEach { childNode ->
                appendMarkdown(
                    markdownText = markdownText,
                    node = childNode,
                    depth = depth + 1,
                    linkColor = linkColor,
                    linkInteractionListener = linkInteractionListener,
                    onImage = onImage,
                    onCheckbox = onCheckbox,
                    maxWidth = maxWidth,
                    bulletColor = bulletColor,
                    headlineLarge = headlineLarge,
                    headlineMedium = headlineMedium,
                    headlineSmall = headlineSmall
                )
            }
        }

        GFMElementTypes.STRIKETHROUGH -> {
            withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                node.children.filter { it.type != GFMTokenTypes.TILDE }.forEach { childNode ->
                    appendMarkdown(
                        markdownText = markdownText,
                        node = childNode,
                        depth = depth + 1,
                        linkColor = linkColor,
                        linkInteractionListener = linkInteractionListener,
                        onImage = onImage,
                        onCheckbox = onCheckbox,
                        maxWidth = maxWidth,
                        bulletColor = bulletColor,
                        headlineLarge = headlineLarge,
                        headlineMedium = headlineMedium,
                        headlineSmall = headlineSmall
                    )
                }
            }
        }

        MarkdownElementTypes.BLOCK_QUOTE -> {
            val quoteColor = Color.Black.copy(alpha = 0.7f)
            val borderColor = Color.Gray
            val quotePadding = 16.sp

            node.children.forEach { childNode ->
                if (childNode.type != MarkdownTokenTypes.BLOCK_QUOTE) {
                    val quoteContent = buildAnnotatedString {
                        appendMarkdown(
                            markdownText = markdownText,
                            node = childNode,
                            depth = depth + 1,
                            linkColor = linkColor,
                            linkInteractionListener = linkInteractionListener,
                            onImage = onImage,
                            onCheckbox = onCheckbox,
                            maxWidth = maxWidth,
                            bulletColor = bulletColor,
                            headlineLarge = headlineLarge,
                            headlineMedium = headlineMedium,
                            headlineSmall = headlineSmall
                        )
                    }
                    val lines = quoteContent.split("\n")
                    lines.forEachIndexed { index, line ->
                        if (index > 0) append("\n")
                        withStyle(style = ParagraphStyle(textIndent = TextIndent(firstLine = 16.sp, restLine = quotePadding))) {
                            withStyle(style = SpanStyle(color = borderColor)) {
                            }
                            withStyle(style = SpanStyle(color = quoteColor)) {
                                append(line)
                            }
                        }
                    }
                }
            }
        }
        else -> {
            append(node.getTextInNode(markdownText).toString())
        }
    }
    return this
}
