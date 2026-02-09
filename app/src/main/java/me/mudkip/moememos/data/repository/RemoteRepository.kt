package me.mudkip.moememos.data.repository

import com.skydoves.sandwich.ApiResponse
import me.mudkip.moememos.data.model.Memo
import me.mudkip.moememos.data.model.MemoVisibility
import me.mudkip.moememos.data.model.Resource
import me.mudkip.moememos.data.model.User
import okhttp3.MediaType

abstract class RemoteRepository {
    abstract suspend fun listMemos(): ApiResponse<List<Memo>>
    abstract suspend fun listArchivedMemos(): ApiResponse<List<Memo>>
    abstract suspend fun listWorkspaceMemos(pageSize: Int, pageToken: String?): ApiResponse<Pair<List<Memo>, String?>>

    abstract suspend fun createMemo(
        content: String,
        visibility: MemoVisibility,
        resourceRemoteIds: List<String>,
        tags: List<String>? = null
    ): ApiResponse<Memo>

    abstract suspend fun updateMemo(
        remoteId: String,
        content: String? = null,
        resourceRemoteIds: List<String>? = null,
        visibility: MemoVisibility? = null,
        tags: List<String>? = null,
        pinned: Boolean? = null
    ): ApiResponse<Memo>

    abstract suspend fun deleteMemo(remoteId: String): ApiResponse<Unit>
    abstract suspend fun archiveMemo(remoteId: String): ApiResponse<Unit>
    abstract suspend fun restoreMemo(remoteId: String): ApiResponse<Unit>

    abstract suspend fun listTags(): ApiResponse<List<String>>
    abstract suspend fun listResources(): ApiResponse<List<Resource>>

    abstract suspend fun createResource(
        filename: String,
        type: MediaType?,
        content: ByteArray,
        memoRemoteId: String? = null
    ): ApiResponse<Resource>

    abstract suspend fun deleteResource(remoteId: String): ApiResponse<Unit>
    abstract suspend fun getCurrentUser(): ApiResponse<User>
}
