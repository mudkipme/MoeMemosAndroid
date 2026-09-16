package me.mudkip.moememos.viewmodel

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.skydoves.sandwich.ApiResponse
import com.skydoves.sandwich.suspendOnSuccess
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    application: Application,
    private val memoService: MemoService
) : AndroidViewModel(application) {
    private val context: Application get() = getApplication()
    val draft = context.settingsDataStore.data.map { settings ->
        settings.usersList.firstOrNull { it.accountKey == settings.currentUser }?.settings?.draft
    }
    val autosaveEnabled = context.settingsDataStore.data.map { settings ->
        settings.usersList.firstOrNull { it.accountKey == settings.currentUser }?.settings?.autosave ?: false
    }
    var uploadResources = mutableStateListOf<ResourceEntity>()

    // Identifier of the memo row that autosave writes to; null until the first autosave creates it.
    @Volatile
    var autosaveIdentifier: String? = null
    private val autosaveMutex = Mutex()

    suspend fun autosave(content: String, visibility: MemoVisibility, tags: List<String>, clearDraftOnCreate: Boolean = false): ApiResponse<MemoEntity> = autosaveMutex.withLock {
        withContext(NonCancellable) {
            val repository = memoService.getRepository()
            val identifier = autosaveIdentifier
            if (identifier == null) {
                repository.createMemo(content, visibility, uploadResources, tags, deferPush = true).also { response ->
                    response.suspendOnSuccess {
                        autosaveIdentifier = data.identifier
                        if (clearDraftOnCreate) {
                            updateDraft("")
                        }
                    }
                }
            } else {
                repository.updateMemo(identifier, content, null, visibility, tags, deferPush = true)
            }
        }
    }

    suspend fun flushAutosave(content: String, visibility: MemoVisibility, tags: List<String>, clearDraftOnCreate: Boolean = false): ApiResponse<MemoEntity> {
        val response = autosave(content, visibility, tags, clearDraftOnCreate)
        response.suspendOnSuccess {
            autosaveIdentifier?.let { memoService.getRepository().flushPendingPush(it) }
            WidgetUpdater.updateWidgets(getApplication())
        }
        return response
    }

    suspend fun discardEmptyAutosave() = autosaveMutex.withLock {
        withContext(NonCancellable) {
            val identifier = autosaveIdentifier ?: return@withContext
            memoService.getRepository().deleteMemo(identifier)
            autosaveIdentifier = null
        }
    }

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
        // Held so an autosave cannot create the memo row while the file is being copied, which
        // would leave this resource unlinked (autosave updates never re-send resources).
        autosaveMutex.withLock {
            try {
                val mimeType = context.contentResolver.getType(uri)
                val extension = mimeType?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) }
                val filename = queryDisplayName(uri)
                    ?: ("attachment_${UUID.randomUUID()}" + if (extension.isNullOrBlank()) "" else ".$extension")

                memoService.getRepository()
                    .createResource(filename, mimeType?.toMediaTypeOrNull(), uri, memoIdentifier ?: autosaveIdentifier)
                    .suspendOnSuccess {
                        uploadResources.add(data)
                    }
            } catch (e: Exception) {
                ApiResponse.Failure.Exception(e)
            }
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
