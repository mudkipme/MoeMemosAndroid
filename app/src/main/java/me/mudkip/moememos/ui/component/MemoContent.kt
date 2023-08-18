package me.mudkip.moememos.ui.component

import android.net.Uri
import androidx.compose.foundation.layout.*

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import me.mudkip.moememos.data.model.Memo
import me.mudkip.moememos.viewmodel.LocalUserState
import kotlin.math.ceil

@Composable
fun MemoContent(
    memo: Memo,
    checkboxChange: (checked: Boolean, startOffset: Int, endOffset: Int) -> Unit = { _, _, _ -> }
) {
    Column(modifier = Modifier.padding(end = 15.dp)) {
        Markdown(memo.content,
            modifier = Modifier.padding(bottom = 10.dp),
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
                                        url = imageList[index].uri(LocalUserState.current.host).toString(),
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
}