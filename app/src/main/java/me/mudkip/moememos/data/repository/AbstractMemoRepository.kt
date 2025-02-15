package me.mudkip.moememos.data.repository

import com.skydoves.sandwich.ApiResponse
import me.mudkip.moememos.data.model.Memo
import me.mudkip.moememos.data.model.MemoVisibility
import me.mudkip.moememos.data.model.Resource
import me.mudkip.moememos.data.model.User
import okhttp3.MediaType

abstract class AbstractMemoRepository {
    abstract suspend fun listMemos(): ApiResponse<List<Memo>>
    abstract suspend fun listArchivedMemos(): ApiResponse<List<Memo>>
    abstract suspend fun listWorkspaceMemos(pageSize: Int, pageToken: String?): ApiResponse<Pair<List<Memo>, String?>>
    abstract suspend fun createMemo(content: String, visibility: MemoVisibility, resources: List<Resource>, tags: List<String>? = null): ApiResponse<Memo>
    abstract suspend fun updateMemo(identifier: String, content: String? = null, resources: List<Resource>? = null, visibility: MemoVisibility? = null, tags: List<String>? = null, pinned: Boolean? = null): ApiResponse<Memo>
    abstract suspend fun deleteMemo(identifier: String): ApiResponse<Unit>
    abstract suspend fun archiveMemo(identifier: String): ApiResponse<Unit>
    abstract suspend fun restoreMemo(identifier: String): ApiResponse<Unit>

    abstract suspend fun listTags(): ApiResponse<List<String>>
    abstract suspend fun deleteTag(name: String): ApiResponse<Unit>

    abstract suspend fun listResources(): ApiResponse<List<Resource>>
    abstract suspend fun createResource(filename: String, type: MediaType?, content: ByteArray, memoIdentifier: String? = null): ApiResponse<Resource>
    abstract suspend fun deleteResource(identifier: String): ApiResponse<Unit>

    abstract suspend fun getCurrentUser(): ApiResponse<User>
}