package me.mudkip.moememos.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.runtime.mutableStateListOf
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skydoves.sandwich.ApiResponse
import com.skydoves.sandwich.suspendOnSuccess
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import me.mudkip.moememos.data.model.Memo
import me.mudkip.moememos.data.model.MemosVisibility
import me.mudkip.moememos.data.model.Resource
import me.mudkip.moememos.data.repository.MemoRepository
import me.mudkip.moememos.data.repository.ResourceRepository
import me.mudkip.moememos.ext.DataStoreKeys
import me.mudkip.moememos.ext.dataStore
import okhttp3.MediaType.Companion.toMediaType
import java.io.ByteArrayOutputStream
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class MemoInputViewModel @Inject constructor(
    private val application: Application,
    private val memoRepository: MemoRepository,
    private val resourceRepository: ResourceRepository
) : ViewModel() {
    val draft = application.applicationContext.dataStore.data.map { it[DataStoreKeys.Draft.key] }
    var uploadResources = mutableStateListOf<Resource>()

    suspend fun createMemo(content: String, visibility: MemosVisibility): ApiResponse<Memo> = withContext(viewModelScope.coroutineContext) {
        memoRepository.createMemo(content, uploadResources.map { it.id }, visibility)
    }

    suspend fun editMemo(memoId: Long, content: String, visibility: MemosVisibility): ApiResponse<Memo> = withContext(viewModelScope.coroutineContext) {
        memoRepository.editMemo(memoId, content, uploadResources.map { it.id }, visibility)
    }

    suspend fun updateTag(content: String): ApiResponse<String> = withContext(viewModelScope.coroutineContext) {
        memoRepository.updateTag(content)
    }

    fun updateDraft(content: String) = runBlocking {
        application.applicationContext.dataStore.edit {
            it[DataStoreKeys.Draft.key] = content
        }
    }

    suspend fun upload(bitmap: Bitmap): ApiResponse<Resource> = withContext(viewModelScope.coroutineContext) {
        val bos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, bos)
        val bytes = bos.toByteArray()
        resourceRepository.uploadResource(bytes, UUID.randomUUID().toString() + ".jpg", "image/jpeg".toMediaType()).suspendOnSuccess {
            uploadResources.add(data)
        }
    }

    fun deleteResource(resourceId: Long) = viewModelScope.launch {
        resourceRepository.deleteResource(resourceId).suspendOnSuccess {
            uploadResources.removeIf { it.id == resourceId }
        }
    }
}