package me.mudkip.moememos.ui.component

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Attachment
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import me.mudkip.moememos.R
import me.mudkip.moememos.data.model.Resource
import me.mudkip.moememos.ext.string
import me.mudkip.moememos.viewmodel.LocalUserState

@Composable
fun Attachment(
    resource: Resource
) {
    val uriHandler = LocalUriHandler.current
    val userStateViewModel = LocalUserState.current

    AssistChip(
        modifier = Modifier.padding(bottom = 10.dp),
        onClick = {
            uriHandler.openUri(resource.uri.toString())
        },
        label = { Text(resource.filename) },
        leadingIcon = {
            Icon(
                Icons.Outlined.Attachment,
                contentDescription = R.string.attachment.string,
                Modifier.size(AssistChipDefaults.IconSize)
            )
        }
    )
}