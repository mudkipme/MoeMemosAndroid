package me.mudkip.moememos.ui.component

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import coil.annotation.ExperimentalCoilApi
import coil.compose.AsyncImage
import coil.imageLoader
import timber.log.Timber
import java.io.File

@OptIn(ExperimentalCoilApi::class)
@Composable
fun MemoImage(
    url: String,
    modifier: Modifier = Modifier
) {
    var diskCacheFile: File? by remember { mutableStateOf(null) }
    val context = LocalContext.current

    AsyncImage(
        model = url,
        contentDescription = null,
        modifier = modifier.clickable {
            diskCacheFile?.let {
                val fileUri: Uri = try {
                    FileProvider.getUriForFile(context, context.packageName + ".fileprovider", it)
                } catch (e: Throwable) {
                    Timber.d(e)
                    null
                } ?: return@let

                val intent = Intent().apply {
                    action = Intent.ACTION_VIEW
                    setDataAndType(fileUri, "image/*")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                try {
                    context.startActivity(intent)
                } catch (e: Throwable) {
                    Timber.d(e)
                }
            }
        },
        contentScale = ContentScale.Crop,
        onSuccess = { state ->
            val diskCache = context.imageLoader.diskCache
            val diskCacheKey = state.result.diskCacheKey

            if (diskCache != null && diskCacheKey != null) {
                diskCacheFile = diskCache[diskCacheKey]?.data?.toFile()
            }
        }
    )
}