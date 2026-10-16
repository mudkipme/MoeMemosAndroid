package me.mudkip.moememos.ui.component

import android.content.Intent

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import me.mudkip.moememos.R
import me.mudkip.moememos.data.local.entity.ResourceEntity
import me.mudkip.moememos.data.model.MemoRepresentable
import me.mudkip.moememos.ext.string
import me.mudkip.moememos.ui.page.common.LocalRootNavController
import me.mudkip.moememos.ui.page.common.RouteName
import me.mudkip.moememos.ui.media.MediaViewerActivity
import me.mudkip.moememos.util.extractPreviewContent
import me.mudkip.moememos.viewmodel.LocalUserState
import java.net.URLEncoder
import kotlin.math.ceil

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
        memo.location?.let { MemoLocationRow(it) }

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

@Composable
fun MemoResourceContent(memo: MemoRepresentable) {
    val cols = 3
    val context = LocalContext.current
    val imageList = memo.resources.filter { it.mimeType?.startsWith("image/") == true }
    val imageUrls = remember(imageList) {
        imageList.map { resource -> resource.localUri ?: resource.uri }
    }
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
                                resourceIdentifier = (imageList[index] as? ResourceEntity)?.identifier,
                                onClick = {
                                    context.startActivity(
                                        Intent(context, MediaViewerActivity::class.java).apply {
                                            putExtra(MediaViewerActivity.EXTRA_IMAGE_URLS, imageUrls.toTypedArray())
                                            putExtra(MediaViewerActivity.EXTRA_INITIAL_INDEX, index)
                                            putExtra(MediaViewerActivity.EXTRA_CAPTION, memo.content)
                                        }
                                    )
                                }
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
