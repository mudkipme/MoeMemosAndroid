package me.mudkip.moememos.ui.component

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import me.mudkip.moememos.R
import me.mudkip.moememos.data.api.MemosV0Memo
import me.mudkip.moememos.ext.string
import me.mudkip.moememos.viewmodel.LocalUserState
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.parser.MarkdownParser
import kotlin.math.ceil

@Composable
fun MemoContent(
    memo: MemosV0Memo,
    previewMode: Boolean = false,
    checkboxChange: (checked: Boolean, startOffset: Int, endOffset: Int) -> Unit = { _, _, _ -> }
) {
    var viewContentExpand by rememberSaveable { mutableStateOf(false) }

    val (text, previewed) = remember(memo.content, previewMode) {
        if (previewMode) {
            extractPreviewContent(markdownText = memo.content)
        } else {
            Pair(memo.content, false)
        }
    }

    Column(
        modifier = Modifier.padding(start = 15.dp, end = 15.dp, bottom = 10.dp)
    ) {
        if (viewContentExpand) {
            Markdown(
                memo.content,
                imageContent = { url ->
                    var uri = Uri.parse(url)
                    if (uri.scheme == null) {
                        uri = Uri.parse(LocalUserState.current.host).buildUpon()
                            .path(url).build()
                    }

                    MemoImage(
                        url = uri.toString(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(8.dp))
                    )
                },
                checkboxChange = checkboxChange
            )
        } else {
            Markdown(
                text,
                imageContent = { url ->
                    var uri = Uri.parse(url)
                    if (uri.scheme == null) {
                        uri = Uri.parse(LocalUserState.current.host).buildUpon()
                            .path(url).build()
                    }

                    MemoImage(
                        url = uri.toString(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(8.dp))
                    )
                },
                checkboxChange = checkboxChange
            )
        }

        MemoResourceContent(memo)

        if (previewed) {
            AssistChip(
                onClick = {
                    viewContentExpand = !viewContentExpand
                },
                label = { Text( if (viewContentExpand) R.string.collapse.string else R.string.expand.string) },
                trailingIcon = {
                    Icon(
                        if (viewContentExpand) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                        contentDescription = if (viewContentExpand) R.string.collapse.string else R.string.expand.string,
                        modifier = Modifier.size(AssistChipDefaults.IconSize),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
                border = BorderStroke(
                    width = 0.dp,
                    color = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    }
}

fun extractPreviewContent(markdownText: String, maxLength: Int = 140): Pair<String, Boolean> {
    val node = MarkdownParser(GFMFlavourDescriptor()).parse(
        MarkdownElementTypes.MARKDOWN_FILE,
        markdownText,
        true
    )

    val result = StringBuilder()
    var firstImage: String? = null

    fun extractNodeContent(child: ASTNode, remainingLength: Int): Int {
        when (child.type) {
            MarkdownElementTypes.PARAGRAPH,
            MarkdownElementTypes.EMPH,
            MarkdownElementTypes.STRONG,
            MarkdownElementTypes.LIST_ITEM,
            MarkdownElementTypes.ORDERED_LIST,
            MarkdownElementTypes.UNORDERED_LIST -> {
                var innerRemainingLength = remainingLength
                for (grandChild in child.children) {
                    innerRemainingLength = extractNodeContent(grandChild, innerRemainingLength)
                    if (innerRemainingLength <= 0 && firstImage != null) break
                }
                return innerRemainingLength
            }

            MarkdownElementTypes.INLINE_LINK,
            MarkdownElementTypes.FULL_REFERENCE_LINK,
            MarkdownElementTypes.SHORT_REFERENCE_LINK -> {
                val content = markdownText.substring(child.startOffset, child.endOffset)
                result.append(content)
                return if (remainingLength - content.length < 0) 0 else
                    remainingLength - content.length
            }

            MarkdownElementTypes.IMAGE -> {
                if (firstImage == null) {
                    firstImage = markdownText.substring(child.startOffset, child.endOffset)
                }
                return remainingLength
            }

            else -> {
                val content = markdownText.substring(child.startOffset, child.endOffset)
                result.append(content.take(remainingLength))
                return remainingLength - content.take(remainingLength).length
            }
        }
    }

    var remainingLength = maxLength
    for (child in node.children) {
        remainingLength = extractNodeContent(child, remainingLength)
        if (remainingLength <= 0 && firstImage != null) break
    }

    if (firstImage != null && !result.contains(firstImage!!)) {
        result.append(firstImage)
    }

    return Pair(result.toString(), remainingLength <= 0)
}

@Composable
fun MemoResourceContent(memo: MemosV0Memo) {
    val cols = 3
    memo.resourceList?.let { resourceList ->
        val imageList = resourceList.filter { it.type.startsWith("image/") }
        if (imageList.isNotEmpty()) {
            val rows = ceil(imageList.size.toFloat() / cols).toInt()
            for (rowIndex in 0 until rows) {
                Row {
                    for (colIndex in 0 until cols) {
                        val index = rowIndex * cols + colIndex
                        if (index < imageList.size) {
                            Box(modifier = Modifier.fillMaxWidth(1f / (cols - colIndex))) {
                                MemoImage(
                                    url = imageList[index].uri(LocalUserState.current.host)
                                        .toString(),
                                    modifier = Modifier
                                        .aspectRatio(1f)
                                        .padding(2.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.fillMaxWidth(1f / cols))
                        }
                    }
                }
            }
        }
        resourceList.filterNot { it.type.startsWith("image/") }.forEach { resource ->
            Attachment(resource)
        }
    }
}