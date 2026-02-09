package me.mudkip.moememos.data.repository

import android.net.Uri
import com.skydoves.sandwich.ApiResponse
import me.mudkip.moememos.data.model.Memo
import me.mudkip.moememos.data.model.MemoVisibility
import me.mudkip.moememos.data.model.Resource
import me.mudkip.moememos.data.model.SyncStatus
import me.mudkip.moememos.data.model.User
import okhttp3.MediaType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

abstract class AbstractMemoRepository {
    private val _syncStatus = MutableStateFlow(SyncStatus())
    open val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    abstract suspend fun listMemos(): ApiResponse<List<Memo>>
    abstract suspend fun listArchivedMemos(): ApiResponse<List<Memo>>
    abstract suspend fun createMemo(content: String, visibility: MemoVisibility, resources: List<Resource>, tags: List<String>? = null): ApiResponse<Memo>
    abstract suspend fun updateMemo(identifier: String, content: String? = null, resources: List<Resource>? = null, visibility: MemoVisibility? = null, tags: List<String>? = null, pinned: Boolean? = null): ApiResponse<Memo>
    abstract suspend fun deleteMemo(identifier: String): ApiResponse<Unit>
    abstract suspend fun archiveMemo(identifier: String): ApiResponse<Unit>
    abstract suspend fun restoreMemo(identifier: String): ApiResponse<Unit>

    abstract suspend fun listTags(): ApiResponse<List<String>>

    abstract suspend fun listResources(): ApiResponse<List<Resource>>
    abstract suspend fun createResource(filename: String, type: MediaType?, content: ByteArray, memoIdentifier: String? = null): ApiResponse<Resource>
    abstract suspend fun deleteResource(identifier: String): ApiResponse<Unit>

    abstract suspend fun getCurrentUser(): ApiResponse<User>

    open suspend fun cacheResourceFile(identifier: String, downloadedUri: Uri): ApiResponse<Unit> {
        return ApiResponse.Success(Unit)
    }

    open suspend fun sync(): ApiResponse<Unit> {
        return ApiResponse.Success(Unit)
    }
}
