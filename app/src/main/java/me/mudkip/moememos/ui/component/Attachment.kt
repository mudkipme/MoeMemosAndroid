package me.mudkip.moememos.ui.component

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Attachment
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import me.mudkip.moememos.data.model.Resource
import me.mudkip.moememos.viewmodel.LocalUserState

@Composable
fun Attachment(
    resource: Resource
) {
    val uriHandler = LocalUriHandler.current
    val userStateViewModel = LocalUserState.current

    TextButton(
        modifier = Modifier.padding(bottom = 10.dp),
        onClick = {
            uriHandler.openUri(resource.uri(userStateViewModel.host).toString())
        }
    ) {
        Icon(Icons.Outlined.Attachment, contentDescription = "Attachment", modifier = Modifier.padding(end = 10.dp))
        Text(resource.filename)
    }
}