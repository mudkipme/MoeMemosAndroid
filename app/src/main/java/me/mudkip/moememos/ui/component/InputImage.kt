package me.mudkip.moememos.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.zIndex
import coil.ImageLoader
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import me.mudkip.moememos.R
import me.mudkip.moememos.data.api.MemosV0Resource
import me.mudkip.moememos.ext.string
import me.mudkip.moememos.viewmodel.LocalUserState
import me.mudkip.moememos.viewmodel.MemoInputViewModel

@Composable
fun InputImage(
    resource: MemosV0Resource,
    inputViewModel: MemoInputViewModel
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val userStateViewModel = LocalUserState.current

    Box {
        AsyncImage(
            model = resource.uri(LocalUserState.current.host).toString(),
            imageLoader = ImageLoader.Builder(context).okHttpClient(userStateViewModel.okHttpClient).build(),
            contentDescription = null,
            modifier = Modifier
                .fillMaxHeight()
                .aspectRatio(1f)
                .zIndex(1f)
                .clip(RoundedCornerShape(8.dp))
                .clickable {
                    menuExpanded = true
                },
            contentScale = ContentScale.Crop
        )
        DropdownMenu(expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false },
            properties = PopupProperties(focusable = false)
        ) {
            DropdownMenuItem(
                text = { Text(R.string.remove.string) },
                onClick = {
                    scope.launch {
                        inputViewModel.deleteResource(resource.id)
                        menuExpanded = false
                    }
                },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = null
                    )
                })
        }
    }
}