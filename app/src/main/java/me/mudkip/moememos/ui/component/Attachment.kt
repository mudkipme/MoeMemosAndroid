package me.mudkip.moememos.ui.component

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Attachment
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.PopupProperties
import androidx.core.net.toUri
import com.skydoves.sandwich.ApiResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.mudkip.moememos.MoeMemosFileProvider
import me.mudkip.moememos.R
import me.mudkip.moememos.data.local.entity.ResourceEntity
import me.mudkip.moememos.data.model.ResourceRepresentable
import me.mudkip.moememos.ext.string
import me.mudkip.moememos.viewmodel.LocalMemos
import me.mudkip.moememos.viewmodel.LocalUserState
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.File

@Composable
fun Attachment(
    resource: ResourceRepresentable,
    onRemove: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val memosViewModel = LocalMemos.current
    val userStateViewModel = LocalUserState.current
    val scope = rememberCoroutineScope()
    var menuExpanded by remember { mutableStateOf(false) }
    var opening by remember { mutableStateOf(false) }

    fun openAttachment() {
        if (opening) {
            return
        }
        scope.launch {
            opening = true
            try {
                val localFile = resolveAttachmentFile(
                    context = context,
                    resource = resource,
                    okHttpClient = userStateViewModel.okHttpClient,
                    cacheCanonical = { resourceIdentifier, downloadedUri ->
                        val result = memosViewModel.cacheResourceFile(resourceIdentifier, downloadedUri)
                        if (result is ApiResponse.Success) {
                            memosViewModel.getResourceById(resourceIdentifier)
                        } else {
                            null
                        }
                    }
                )
                if (localFile == null) {
                    Toast.makeText(context, R.string.failed_to_open_attachment.string, Toast.LENGTH_SHORT).show()
                    return@launch
                }
                val fileUri = MoeMemosFileProvider.getFileUri(context, localFile)
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = resolveMimeType(resource, localFile)
                    putExtra(Intent.EXTRA_STREAM, fileUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    clipData = ClipData.newUri(
                        context.contentResolver,
                        resource.filename.ifBlank { "attachment" },
                        fileUri
                    )
                }
                context.startActivity(Intent.createChooser(shareIntent, null))
            } catch (e: Throwable) {
                Timber.d(e)
                Toast.makeText(context, R.string.failed_to_open_attachment.string, Toast.LENGTH_SHORT).show()
            } finally {
                opening = false
                menuExpanded = false
            }
        }
    }

    AssistChip(
        enabled = !opening,
        onClick = {
            if (onRemove == null) {
                openAttachment()
            } else {
                menuExpanded = true
            }
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

    if (onRemove != null) {
        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false },
            properties = PopupProperties(focusable = false)
        ) {
            DropdownMenuItem(
                text = { Text(R.string.open.string) },
                onClick = {
                    openAttachment()
                },
                leadingIcon = {
                    Icon(
                        Icons.AutoMirrored.Outlined.OpenInNew,
                        contentDescription = null
                    )
                }
            )
            DropdownMenuItem(
                text = { Text(R.string.remove.string) },
                onClick = {
                    onRemove()
                    menuExpanded = false
                },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = null
                    )
                }
            )
        }
    }
}

private suspend fun resolveAttachmentFile(
    context: Context,
    resource: ResourceRepresentable,
    okHttpClient: OkHttpClient,
    cacheCanonical: suspend (resourceIdentifier: String, downloadedUri: android.net.Uri) -> ResourceEntity?
): File? {
    existingLocalFile(resource)?.let { return it }

    val uri = resource.uri.toUri()
    if (uri.scheme != "http" && uri.scheme != "https") {
        return null
    }

    val downloaded = downloadAttachment(context, okHttpClient, resource.uri, resource.filename) ?: return null
    val resourceEntity = resource as? ResourceEntity ?: return downloaded

    val cached = cacheCanonical(resourceEntity.identifier, downloaded.toUri())
    val canonical = cached?.localUri
        ?.toUri()
        ?.takeIf { it.scheme == "file" }
        ?.path
        ?.let(::File)
        ?.takeIf { it.exists() }

    downloaded.delete()
    return canonical
}

private fun existingLocalFile(resource: ResourceRepresentable): File? {
    val local = (resource.localUri ?: resource.uri).toUri()
    if (local.scheme != "file") {
        return null
    }
    val path = local.path ?: return null
    return File(path).takeIf { it.exists() }
}

private suspend fun downloadAttachment(
    context: Context,
    okHttpClient: OkHttpClient,
    url: String,
    filename: String
): File? = withContext(Dispatchers.IO) {
    val request = Request.Builder().url(url).get().build()
    okHttpClient.newCall(request).execute().use { response ->
        if (!response.isSuccessful) {
            return@withContext null
        }
        val body = response.body
        val dir = File(context.cacheDir, "image_cache").also { it.mkdirs() }
        val suffix = "_${sanitizeFilename(filename.ifBlank { "attachment" })}"
        val target = File.createTempFile("attachment_", suffix, dir)
        body.byteStream().use { input ->
            target.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        target
    }
}

private fun sanitizeFilename(filename: String): String {
    return filename.replace(Regex("[^A-Za-z0-9._-]"), "_")
}

private fun resolveMimeType(resource: ResourceRepresentable, file: File): String {
    resource.mimeType?.takeIf { it.isNotBlank() }?.let { return it }
    val ext = file.extension.lowercase()
    if (ext.isBlank()) {
        return "*/*"
    }
    return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "*/*"
}
