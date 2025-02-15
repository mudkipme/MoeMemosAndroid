package me.mudkip.moememos.data.repository

import com.skydoves.sandwich.ApiResponse
import com.skydoves.sandwich.mapSuccess
import me.mudkip.moememos.data.api.MemosRowStatus
import me.mudkip.moememos.data.api.MemosV0Api
import me.mudkip.moememos.data.api.MemosV0CreateMemoInput
import me.mudkip.moememos.data.api.MemosV0DeleteTagInput
import me.mudkip.moememos.data.api.MemosV0Memo
import me.mudkip.moememos.data.api.MemosV0PatchMemoInput
import me.mudkip.moememos.data.api.MemosV0Resource
import me.mudkip.moememos.data.api.MemosV0UpdateMemoOrganizerInput
import me.mudkip.moememos.data.api.MemosV0UpdateTagInput
import me.mudkip.moememos.data.api.MemosVisibility
import me.mudkip.moememos.data.constant.MoeMemosException
import me.mudkip.moememos.data.model.Account
import me.mudkip.moememos.data.model.Memo
import me.mudkip.moememos.data.model.MemoVisibility
import me.mudkip.moememos.data.model.Resource
import me.mudkip.moememos.data.model.User
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.time.Instant

class MemosV0Repository (
    private val memosApi: MemosV0Api,
    private val account: Account.MemosV0,
) : AbstractMemoRepository() {
    private fun convertResource(resource: MemosV0Resource): Resource {
        return Resource(
            identifier = resource.id.toString(),
            date = Instant.ofEpochSecond(resource.createdTs),
            filename = resource.filename,
            uri = resource.uri(account.info.host),
            mimeType = resource.type.toMediaTypeOrNull()
        )
    }

    private fun convertMemo(memo: MemosV0Memo): Memo {
        return Memo(
            identifier = memo.id.toString(),
            content = memo.content,
            date = Instant.ofEpochSecond(memo.createdTs),
            pinned = memo.pinned,
            visibility = memo.visibility.toMemoVisibility(),
            resources = memo.resourceList?.map { convertResource(it) } ?: emptyList(),
            tags = emptyList(),
            creator = memo.creatorName?.let { User(memo.creatorId.toString(), it) }
        )
    }

    override suspend fun listMemos(): ApiResponse<List<Memo>> {
        return memosApi.listMemo(rowStatus = MemosRowStatus.NORMAL).mapSuccess {
            this.map { convertMemo(it) }
        }
    }

    override suspend fun listArchivedMemos(): ApiResponse<List<Memo>> {
        return memosApi.listMemo(rowStatus = MemosRowStatus.ARCHIVED).mapSuccess {
            this.map { convertMemo(it) }
        }
    }

    override suspend fun createMemo(
        content: String,
        visibility: MemoVisibility,
        resources: List<Resource>,
        tags: List<String>?
    ): ApiResponse<Memo> {
        val result = memosApi.createMemo(MemosV0CreateMemoInput(content, resourceIdList = resources.map { it.identifier.toLong() }, visibility = MemosVisibility.fromMemoVisibility(visibility))).mapSuccess {
            convertMemo(this)
        }
        tags?.forEach { tag ->
            memosApi.updateTag(MemosV0UpdateTagInput(tag))
        }
        return result
    }

    override suspend fun updateMemo(
        identifier: String,
        content: String?,
        resources: List<Resource>?,
        visibility: MemoVisibility?,
        tags: List<String>?,
        pinned: Boolean?
    ): ApiResponse<Memo> {
        var result: ApiResponse<Memo> = ApiResponse.exception(MoeMemosException.invalidParameter)
        pinned?.let {
            result = memosApi.updateMemoOrganizer(identifier.toLong(), MemosV0UpdateMemoOrganizerInput(pinned = it)).mapSuccess {
                convertMemo(this)
            }
            if (result !is ApiResponse.Success<Memo>) {
                return result
            }
        }
        content?.let {
            val memosVisibility = visibility?.let { v -> MemosVisibility.fromMemoVisibility(v) }
            val resourceIdList = resources?.map { resource -> resource.identifier.toLong() }
            result = memosApi.patchMemo(identifier.toLong(), MemosV0PatchMemoInput(id = identifier.toLong(), content = it, visibility = memosVisibility, resourceIdList = resourceIdList)).mapSuccess {
                convertMemo(this)
            }
        }
        tags?.forEach { tag ->
            memosApi.updateTag(MemosV0UpdateTagInput(tag))
        }
        return result
    }

    override suspend fun deleteMemo(identifier: String): ApiResponse<Unit> {
        return memosApi.deleteMemo(identifier.toLong())
    }

    override suspend fun archiveMemo(identifier: String): ApiResponse<Unit> {
        return memosApi.patchMemo(identifier.toLong(), MemosV0PatchMemoInput(id = identifier.toLong(), rowStatus = MemosRowStatus.ARCHIVED)).mapSuccess {}
    }

    override suspend fun restoreMemo(identifier: String): ApiResponse<Unit> {
        return memosApi.patchMemo(identifier.toLong(), MemosV0PatchMemoInput(id = identifier.toLong(), rowStatus = MemosRowStatus.NORMAL)).mapSuccess {}
    }

    override suspend fun listTags(): ApiResponse<List<String>> {
        return memosApi.getTags()
    }

    override suspend fun deleteTag(name: String): ApiResponse<Unit> {
        return memosApi.deleteTag(MemosV0DeleteTagInput(name))
    }

    override suspend fun listResources(): ApiResponse<List<Resource>> {
        return memosApi.getResources().mapSuccess {
            this.map { convertResource(it) }
        }
    }

    override suspend fun createResource(
        filename: String,
        type: MediaType?,
        content: ByteArray,
        memoIdentifier: String?
    ): ApiResponse<Resource> {
        val file = MultipartBody.Part.createFormData("file", filename, content.toRequestBody(type))
        return memosApi.uploadResource(file).mapSuccess {
            convertResource(this)
        }
    }

    override suspend fun deleteResource(identifier: String): ApiResponse<Unit> {
        return memosApi.deleteResource(identifier.toLong())
    }

    override suspend fun listWorkspaceMemos(
        pageSize: Int,
        pageToken: String?
    ): ApiResponse<Pair<List<Memo>, String?>> {
        return memosApi.listAllMemo(limit = pageSize, offset = pageToken?.toIntOrNull()).mapSuccess {
            val memos = this.map { convertMemo(it) }
            val nextPageToken = (pageToken?.toIntOrNull() ?: 0) + pageSize
            memos to nextPageToken.toString()
        }
    }

    override suspend fun getCurrentUser(): ApiResponse<User> {
        return memosApi.me().mapSuccess {
            toUser()
        }
    }
}