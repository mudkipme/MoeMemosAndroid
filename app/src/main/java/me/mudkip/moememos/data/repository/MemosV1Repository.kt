package me.mudkip.moememos.data.repository

import com.skydoves.sandwich.ApiResponse
import com.skydoves.sandwich.mapSuccess
import com.skydoves.sandwich.onSuccess
import me.mudkip.moememos.data.api.MemosV1Api
import me.mudkip.moememos.data.api.MemosV1Memo
import me.mudkip.moememos.data.api.MemosV1Resource
import me.mudkip.moememos.data.model.Account
import me.mudkip.moememos.data.model.Memo
import me.mudkip.moememos.data.model.MemoVisibility
import me.mudkip.moememos.data.model.Resource
import me.mudkip.moememos.data.model.User
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull

private const val PAGE_SIZE = 100

class MemosV1Repository(
    private val memosApi: MemosV1Api,
    private val account: Account.MemosV1
): AbstractMemoRepository() {

    private fun convertResource(resource: MemosV1Resource): Resource {
        return Resource(
            identifier = resource.name,
            date = resource.createTime.toInstant(),
            filename = resource.filename,
            uri = resource.uri(account.info.host),
            mimeType = resource.type.toMediaTypeOrNull()
        )
    }

    private fun convertMemo(memo: MemosV1Memo): Memo {
        return Memo(
            identifier = memo.name,
            content = memo.content,
            date = memo.displayTime.toInstant(),
            pinned = memo.pinned,
            visibility = memo.visibility.toMemoVisibility(),
            resources = memo.resources.map { convertResource(it) },
            tags = emptyList(),
            // TODO: parse creator
        )
    }

    override suspend fun listMemos(): ApiResponse<List<Memo>> {
        var nextPageToken = ""
        val memos = arrayListOf<Memo>()

        do {
            val resp = memosApi.listMemos(PAGE_SIZE, nextPageToken, "creator == \"users/${account.info.id}\" && row_status == \"NORMAL\" && order_by_pinned == true")
                .onSuccess { nextPageToken = data.nextPageToken }
                .mapSuccess { this.memos.map { convertMemo(it) } }
            if (resp is ApiResponse.Success) {
                memos.addAll(resp.data)
            } else {
                return resp
            }
        } while (nextPageToken.isNotEmpty())
        return ApiResponse.Success(memos)
    }

    override suspend fun listArchivedMemos(): ApiResponse<List<Memo>> {
        TODO("Not yet implemented")
    }

    override suspend fun listWorkspaceMemos(
        pageSize: Int,
        pageToken: String?
    ): ApiResponse<Pair<List<Memo>, String>> {
        TODO("Not yet implemented")
    }

    override suspend fun createMemo(
        content: String,
        visibility: MemoVisibility,
        resources: List<Resource>
    ): ApiResponse<Memo> {
        TODO("Not yet implemented")
    }

    override suspend fun updateMemo(
        identifier: String,
        content: String?,
        resources: List<Resource>?,
        visibility: MemoVisibility?,
        tags: List<String>?,
        pinned: Boolean?
    ): ApiResponse<Memo> {
        TODO("Not yet implemented")
    }

    override suspend fun deleteMemo(identifier: String): ApiResponse<Unit> {
        TODO("Not yet implemented")
    }

    override suspend fun archiveMemo(identifier: String): ApiResponse<Unit> {
        TODO("Not yet implemented")
    }

    override suspend fun restoreMemo(identifier: String): ApiResponse<Unit> {
        TODO("Not yet implemented")
    }

    override suspend fun listTags(): ApiResponse<List<String>> {
        TODO("Not yet implemented")
    }

    override suspend fun deleteTag(name: String): ApiResponse<Unit> {
        TODO("Not yet implemented")
    }

    override suspend fun listResources(): ApiResponse<List<Resource>> {
        TODO("Not yet implemented")
    }

    override suspend fun createResource(
        filename: String,
        type: MediaType?,
        content: ByteArray,
        memoIdentifier: String?
    ): ApiResponse<Resource> {
        TODO("Not yet implemented")
    }

    override suspend fun deleteResource(identifier: String): ApiResponse<Unit> {
        TODO("Not yet implemented")
    }

    override suspend fun getCurrentUser(): ApiResponse<User> {
        TODO("Not yet implemented")
    }

    override suspend fun logout(): ApiResponse<Unit> {
        TODO("Not yet implemented")
    }
}