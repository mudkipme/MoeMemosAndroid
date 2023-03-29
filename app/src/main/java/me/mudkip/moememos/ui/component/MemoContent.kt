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

        memo.resourceList?.forEach { resource ->
            if (resource.type.startsWith("image/")) {
                MemoImage(
                    url = resource.uri(LocalUserState.current.host).toString(),
                    modifier = Modifier
                        .heightIn(max = 400.dp)
                        .widthIn(min = 100.dp)
                        .padding(bottom = 10.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            } else {
                Attachment(resource)
            }
        }
    }
}