package me.mudkip.moememos.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import me.mudkip.moememos.ui.page.common.LocalRootNavController
import me.mudkip.moememos.ui.page.common.RouteName
import me.mudkip.moememos.R
import me.mudkip.moememos.data.local.entity.ResourceEntity
import me.mudkip.moememos.data.model.MemoRepresentable
import me.mudkip.moememos.ext.string
import me.mudkip.moememos.viewmodel.LocalUserState
import org.intellij.markdown.IElementType
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.flavours.gfm.GFMElementTypes
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.parser.MarkdownParser
import kotlin.math.ceil
import java.net.URLEncoder

@Composable
fun MemoContent(
    memo: MemoRepresentable,
    previewMode: Boolean = false,
    checkboxChange: (checked: Boolean, startOffset: Int, endOffset: Int) -> Unit = { _, _, _ -> },
    onViewMore: (() -> Unit)? = null,
    selectable: Boolean = false,
    onTagClick: ((String) -> Unit)? = null
) {
    val rootNavController = LocalRootNavController.current
    val (text, previewed) = remember(memo.content, previewMode) {
        if (previewMode) {
            extractPreviewContent(markdownText = memo.content)
        } else {
            Pair(memo.content, false)
        }
    }
    val handleTagClick = remember(rootNavController, onTagClick) {
        onTagClick ?: { tag ->
            rootNavController.navigate("${RouteName.TAG}/${URLEncoder.encode(tag, "UTF-8")}") {
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    Column(
        modifier = Modifier.padding(start = 15.dp, end = 15.dp, bottom = 10.dp)
    ) {
        Markdown(
            text,
            imageBaseUrl = LocalUserState.current.host,
            checkboxChange = checkboxChange,
            selectable = selectable,
            onTagClick = handleTagClick
        )

        MemoResourceContent(memo)

        if (previewed && onViewMore != null) {
            Row {
                Text(
                    text = R.string.view_more.string,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium.copy(textDecoration = TextDecoration.Underline),
                    modifier = Modifier.clickable(onClick = onViewMore)
                )
            }
        }
    }
}

private const val PREVIEW_UNBREAKABLE_COST = 100
private enum class PreviewAppendKind {
    NONE,
    TEXT,
    UNBREAKABLE
}

fun extractPreviewContent(markdownText: String, maxLength: Int = 500): Pair<String, Boolean> {
    val node = MarkdownParser(GFMFlavourDescriptor()).parse(
        MarkdownElementTypes.MARKDOWN_FILE,
        markdownText,
        true
    )

    val result = StringBuilder()
    var remainingLength = maxLength
    var truncated = false
    var lastAppendKind = PreviewAppendKind.NONE

    fun appendNodeText(child: ASTNode): Boolean {
        if (remainingLength <= 0) {
            truncated = true
            return false
        }
        val content = markdownText.substring(child.startOffset, child.endOffset)
        if (content.isEmpty()) {
            return true
        }
        if (content.length <= remainingLength) {
            result.append(content)
            remainingLength -= content.length
            lastAppendKind = PreviewAppendKind.TEXT
            return true
        }
        result.append(content.take(remainingLength))
        remainingLength = 0
        truncated = true
        lastAppendKind = PreviewAppendKind.TEXT
        return false
    }

    fun appendUnbreakableNode(child: ASTNode): Boolean {
        if (remainingLength < PREVIEW_UNBREAKABLE_COST) {
            truncated = true
            return false
        }
        result.append(markdownText.substring(child.startOffset, child.endOffset))
        remainingLength -= PREVIEW_UNBREAKABLE_COST
        lastAppendKind = PreviewAppendKind.UNBREAKABLE
        return true
    }

    lateinit var extractNodeContent: (ASTNode) -> Boolean
    lateinit var extractBlockContent: (ASTNode) -> Boolean

    extractNodeContent = { child ->
        if (isUnbreakablePreviewNode(child)) {
            appendUnbreakableNode(child)
        } else if (child.children.isEmpty()) {
            appendNodeText(child)
        } else {
            var allSuccess = true
            for (grandChild in child.children) {
                val success = if (isBreakablePreviewBlock(grandChild.type)) {
                    extractBlockContent(grandChild)
                } else {
                    extractNodeContent(grandChild)
                }
                if (!success) {
                    allSuccess = false
                    break
                }
            }
            allSuccess
        }
    }

    extractBlockContent = { child ->
        var allSuccess = true
        val isParagraph = child.type == MarkdownElementTypes.PARAGRAPH
        for (grandChild in child.children) {
            val success = if (isParagraph) {
                extractNodeContent(grandChild)
            } else if (isBreakablePreviewBlock(grandChild.type)) {
                extractBlockContent(grandChild)
            } else if (isPreviewWhitespaceToken(grandChild) || grandChild.children.isEmpty()) {
                appendNodeText(grandChild)
            } else {
                appendUnbreakableNode(grandChild)
            }
            if (!success) {
                allSuccess = false
                break
            }
        }
        allSuccess
    }

    for (child in node.children) {
        val success = if (isBreakablePreviewBlock(child.type)) {
            extractBlockContent(child)
        } else if (isPreviewWhitespaceToken(child)) {
            appendNodeText(child)
        } else {
            appendUnbreakableNode(child)
        }
        if (!success) {
            break
        }
    }

    if (truncated && lastAppendKind == PreviewAppendKind.TEXT) {
        val preview = result.toString().trimEnd()
        val withEllipsis = if (preview.endsWith("…")) preview else "$preview…"
        return Pair(withEllipsis, true)
    }

    return Pair(result.toString(), truncated)
}

private fun isBreakablePreviewBlock(type: IElementType): Boolean {
    return type == MarkdownElementTypes.MARKDOWN_FILE ||
        type == MarkdownElementTypes.PARAGRAPH ||
        type == MarkdownElementTypes.LIST_ITEM ||
        type == MarkdownElementTypes.BLOCK_QUOTE ||
        type == MarkdownElementTypes.ORDERED_LIST ||
        type == MarkdownElementTypes.UNORDERED_LIST
}

private fun isUnbreakablePreviewNode(node: ASTNode): Boolean {
    return node.type == MarkdownElementTypes.IMAGE ||
        node.type == MarkdownElementTypes.CODE_BLOCK ||
        node.type == MarkdownElementTypes.CODE_FENCE ||
        node.type == GFMElementTypes.TABLE ||
        node.type == MarkdownElementTypes.ATX_1 ||
        node.type == MarkdownElementTypes.ATX_2 ||
        node.type == MarkdownElementTypes.ATX_3 ||
        node.type == MarkdownElementTypes.ATX_4 ||
        node.type == MarkdownElementTypes.ATX_5 ||
        node.type == MarkdownElementTypes.ATX_6 ||
        node.type == MarkdownElementTypes.SETEXT_1 ||
        node.type == MarkdownElementTypes.SETEXT_2 ||
        node.type == MarkdownTokenTypes.HORIZONTAL_RULE ||
        node.type == MarkdownElementTypes.LINK_DEFINITION ||
        node.type.toString().contains("HTML")
}

private fun isPreviewWhitespaceToken(node: ASTNode): Boolean {
    return node.type == MarkdownTokenTypes.EOL || node.type == MarkdownTokenTypes.WHITE_SPACE
}

@Composable
fun MemoResourceContent(memo: MemoRepresentable) {
    val cols = 3

    val imageList = memo.resources.filter { it.mimeType?.startsWith("image/") == true }
    if (imageList.isNotEmpty()) {
        val rows = ceil(imageList.size.toFloat() / cols).toInt()
        for (rowIndex in 0 until rows) {
            Row {
                for (colIndex in 0 until cols) {
                    val index = rowIndex * cols + colIndex
                    if (index < imageList.size) {
                        Box(modifier = Modifier.fillMaxWidth(1f / (cols - colIndex))) {
                            MemoImage(
                                url = imageList[index].localUri ?: imageList[index].uri,
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .padding(2.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                resourceIdentifier = (imageList[index] as? ResourceEntity)?.identifier
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.fillMaxWidth(1f / cols))
                    }
                }
            }
        }
    }
    memo.resources.filterNot { it.mimeType?.startsWith("image/") == true }.forEach { resource ->
        Attachment(resource)
    }
}
