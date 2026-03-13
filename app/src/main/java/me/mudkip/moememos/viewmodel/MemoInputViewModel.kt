package me.mudkip.moememos.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.skydoves.sandwich.ApiResponse
import com.skydoves.sandwich.suspendOnSuccess
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.mudkip.moememos.data.local.entity.MemoEntity
import me.mudkip.moememos.data.local.entity.ResourceEntity
import me.mudkip.moememos.data.model.MemoVisibility
import me.mudkip.moememos.data.service.MemoService
import me.mudkip.moememos.ext.settingsDataStore
import me.mudkip.moememos.widget.WidgetUpdater
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class MemoInputViewModel @Inject constructor(
    @ApplicationContext application: Context,
    private val memoService: MemoService
) : AndroidViewModel(application as Application) {
    private val context = application
    val draft = context.settingsDataStore.data.map { settings ->
        settings.usersList.firstOrNull { it.accountKey == settings.currentUser }?.settings?.draft
    }
    var uploadResources = mutableStateListOf<ResourceEntity>()

    suspend fun createMemo(content: String, visibility: MemoVisibility, tags: List<String>): ApiResponse<MemoEntity> = withContext(viewModelScope.coroutineContext) {
        val response = memoService.getRepository().createMemo(content, visibility, uploadResources, tags)
        // Update widgets when a new memo is created
        response.suspendOnSuccess {
            WidgetUpdater.updateWidgets(getApplication())
        }
        response
    }

    suspend fun editMemo(identifier: String, content: String, visibility: MemoVisibility, tags: List<String>): ApiResponse<MemoEntity> = withContext(viewModelScope.coroutineContext) {
        val response = memoService.getRepository().updateMemo(identifier, content, uploadResources, visibility, tags)
        // Update widgets when a memo is edited
        response.suspendOnSuccess {
            WidgetUpdater.updateWidgets(getApplication())
        }
        response
    }

    fun updateDraft(content: String) {
        viewModelScope.launch(Dispatchers.IO) {
            context.settingsDataStore.updateData { settings ->
                val index = settings.usersList.indexOfFirst { it.accountKey == settings.currentUser }
                if (index == -1) {
                    return@updateData settings
                }
                val users = settings.usersList.toMutableList()
                val user = users[index]
                users[index] = user.copy(settings = user.settings.copy(draft = content))
                settings.copy(usersList = users)
            }
        }
    }

    suspend fun upload(uri: Uri, memoIdentifier: String?): ApiResponse<ResourceEntity> = withContext(Dispatchers.IO) {
        try {
            val mimeType = context.contentResolver.getType(uri)
            val extension = mimeType?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) }
            val filename = queryDisplayName(uri)
                ?: ("attachment_${UUID.randomUUID()}" + if (extension.isNullOrBlank()) "" else ".$extension")

            memoService.getRepository()
                .createResource(filename, mimeType?.toMediaTypeOrNull(), uri, memoIdentifier)
                .suspendOnSuccess {
                    uploadResources.add(data)
                }
        } catch (e: Exception) {
            ApiResponse.Failure.Exception(e)
        }
    }

    private fun queryDisplayName(uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (!cursor.moveToFirst()) {
                    return@use null
                }
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index == -1) {
                    null
                } else {
                    cursor.getString(index)
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    fun deleteResource(resourceIdentifier: String) = viewModelScope.launch {
        memoService.getRepository().deleteResource(resourceIdentifier).suspendOnSuccess {
            uploadResources.removeIf { it.identifier == resourceIdentifier }
        }
    }
}
