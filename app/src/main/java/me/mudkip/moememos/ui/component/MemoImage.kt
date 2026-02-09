package me.mudkip.moememos.ui.component

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import coil3.ImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import me.mudkip.moememos.viewmodel.LocalMemos
import me.mudkip.moememos.viewmodel.LocalUserState
import timber.log.Timber
import java.io.File

@OptIn(ExperimentalCoilApi::class)
@Composable
fun MemoImage(
    url: String,
    modifier: Modifier = Modifier,
    resourceIdentifier: String? = null
) {
    var diskCacheFile: File? by remember { mutableStateOf(null) }
    val context = LocalContext.current
    val userStateViewModel = LocalUserState.current
    val memosViewModel = LocalMemos.current
    val modelUri = remember(url) { url.toUri() }
    val modelFile = remember(url) {
        modelUri.takeIf { it.scheme == "file" }?.path?.let(::File)
    }

    AsyncImage(
        model = url,
        imageLoader = ImageLoader.Builder(context)
            .components {
                add(
                    OkHttpNetworkFetcherFactory(
                        callFactory = { userStateViewModel.okHttpClient }
                    )
                )
            }
            .build(),
        contentDescription = null,
        modifier = modifier.clickable {
            val fileToOpen = diskCacheFile ?: modelFile
            fileToOpen?.let {
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
                val downloadedFile = diskCache.openSnapshot(diskCacheKey)?.data?.toFile()
                diskCacheFile = downloadedFile
                val shouldPersistDownloadedFile = resourceIdentifier != null &&
                    downloadedFile != null &&
                    modelUri.scheme != "file"
                if (shouldPersistDownloadedFile) {
                    memosViewModel.cacheResourceFile(resourceIdentifier, Uri.fromFile(downloadedFile))
                }
            }
        }
    )
}
