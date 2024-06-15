package me.mudkip.moememos.data.repository

import com.skydoves.sandwich.ApiResponse
import me.mudkip.moememos.data.constant.MoeMemosException
import me.mudkip.moememos.data.model.Memo
import me.mudkip.moememos.data.model.MemoVisibility
import me.mudkip.moememos.data.model.Resource
import me.mudkip.moememos.data.model.User
import okhttp3.MediaType

class LocalRepository : AbstractMemoRepository() {
    override suspend fun listMemos(): ApiResponse<List<Memo>> {
        return ApiResponse.exception(MoeMemosException.notLogin)
    }

    override suspend fun listArchivedMemos(): ApiResponse<List<Memo>> {
        return ApiResponse.exception(MoeMemosException.notLogin)
    }

    override suspend fun listWorkspaceMemos(
        pageSize: Int,
        pageToken: String?
    ): ApiResponse<Pair<List<Memo>, String>> {
        return ApiResponse.exception(MoeMemosException.notLogin)
    }

    override suspend fun createMemo(
        content: String,
        visibility: MemoVisibility,
        resources: List<Resource>,
        tags: List<String>?
    ): ApiResponse<Memo> {
        return ApiResponse.exception(MoeMemosException.notLogin)
    }

    override suspend fun updateMemo(
        identifier: String,
        content: String?,
        resources: List<Resource>?,
        visibility: MemoVisibility?,
        tags: List<String>?,
        pinned: Boolean?
    ): ApiResponse<Memo> {
        return ApiResponse.exception(MoeMemosException.notLogin)
    }

    override suspend fun deleteMemo(identifier: String): ApiResponse<Unit> {
        return ApiResponse.exception(MoeMemosException.notLogin)
    }

    override suspend fun archiveMemo(identifier: String): ApiResponse<Unit> {
        return ApiResponse.exception(MoeMemosException.notLogin)
    }

    override suspend fun restoreMemo(identifier: String): ApiResponse<Unit> {
        return ApiResponse.exception(MoeMemosException.notLogin)
    }

    override suspend fun listTags(): ApiResponse<List<String>> {
        return ApiResponse.exception(MoeMemosException.notLogin)
    }

    override suspend fun deleteTag(name: String): ApiResponse<Unit> {
        return ApiResponse.exception(MoeMemosException.notLogin)
    }

    override suspend fun listResources(): ApiResponse<List<Resource>> {
        return ApiResponse.exception(MoeMemosException.notLogin)
    }

    override suspend fun createResource(
        filename: String,
        type: MediaType?,
        content: ByteArray,
        memoIdentifier: String?
    ): ApiResponse<Resource> {
        return ApiResponse.exception(MoeMemosException.notLogin)
    }

    override suspend fun deleteResource(identifier: String): ApiResponse<Unit> {
        return ApiResponse.exception(MoeMemosException.notLogin)
    }

    override suspend fun getCurrentUser(): ApiResponse<User> {
        return ApiResponse.exception(MoeMemosException.notLogin)
    }

    override suspend fun logout(): ApiResponse<Unit> {
        return ApiResponse.Success(Unit)
    }
}