package me.mudkip.moememos.data.repository

import com.skydoves.sandwich.ApiResponse
import com.skydoves.sandwich.getOrNull
import com.skydoves.sandwich.mapSuccess
import com.skydoves.sandwich.onSuccess
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import me.mudkip.moememos.data.api.CreateResourceRequest
import me.mudkip.moememos.data.api.MemosV1Api
import me.mudkip.moememos.data.api.MemosV1CreateMemoRequest
import me.mudkip.moememos.data.api.MemosV1Memo
import me.mudkip.moememos.data.api.MemosV1Resource
import me.mudkip.moememos.data.api.MemosV1SetMemoResourcesRequest
import me.mudkip.moememos.data.api.MemosV1SetMemoResourcesRequestItem
import me.mudkip.moememos.data.api.MemosV1State
import me.mudkip.moememos.data.api.MemosVisibility
import me.mudkip.moememos.data.api.UpdateMemoRequest
import me.mudkip.moememos.data.constant.MoeMemosException
import me.mudkip.moememos.data.model.Account
import me.mudkip.moememos.data.model.Memo
import me.mudkip.moememos.data.model.MemoVisibility
import me.mudkip.moememos.data.model.Resource
import me.mudkip.moememos.data.model.User
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okio.ByteString.Companion.toByteString
import java.util.Date

private const val PAGE_SIZE = 200

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
            resources = memo.attachments.map { convertResource(it) },
            tags = emptyList(),
        )
    }

    private suspend fun listMemosByFilter(state: MemosV1State, parent: String): ApiResponse<List<Memo>> {
        var nextPageToken = ""
        val memos = arrayListOf<Memo>()

        do {
            val resp = memosApi.listMemos(PAGE_SIZE, nextPageToken, state, parent)
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

    private fun getId(identifier: String): String {
        return identifier.substringBefore('|').substringAfterLast('/')
    }

    private fun getName(identifier: String): String {
        return identifier.substringBefore('|')
    }

    override suspend fun listMemos(): ApiResponse<List<Memo>> {
        return listMemosByFilter(MemosV1State.NORMAL, "users/${account.info.id}")
    }

    override suspend fun listArchivedMemos(): ApiResponse<List<Memo>> {
        return listMemosByFilter(MemosV1State.ARCHIVED, "users/${account.info.id}")
    }

    override suspend fun listWorkspaceMemos(
        pageSize: Int,
        pageToken: String?
    ): ApiResponse<Pair<List<Memo>, String?>> {
        val resp = memosApi.listMemos(pageSize, pageToken)
        if (resp !is ApiResponse.Success) {
            return resp.mapSuccess { emptyList<Memo>() to null }
        }
        val users = resp.data.memos.map { getId(it.creator) }.toSet()
        val userResp = coroutineScope {
            users.map { userId ->
                async { memosApi.getUser(userId).getOrNull() }
            }.awaitAll()
        }.filterNotNull()
        val userMap = mapOf(*userResp.map { it.name to it }.toTypedArray())

        return resp
            .mapSuccess { this.memos.map {
                convertMemo(it)
                .copy(creator = userMap[it.creator]?.let { user -> User(user.name, user.displayName, user.createTime.toInstant()) } )
            } to this.nextPageToken.ifEmpty { null } }
    }

    override suspend fun createMemo(
        content: String,
        visibility: MemoVisibility,
        resources: List<Resource>,
        tags: List<String>?
    ): ApiResponse<Memo> {
        val resp = memosApi.createMemo(MemosV1CreateMemoRequest(content, MemosVisibility.fromMemoVisibility(visibility)))
            .mapSuccess { convertMemo(this) }
        if (resp !is ApiResponse.Success || resources.isEmpty()) {
            return resp
        }
        return memosApi.setMemoResources(getId(resp.data.identifier), MemosV1SetMemoResourcesRequest(
            resources.map {
                MemosV1SetMemoResourcesRequestItem(getName(it.identifier))
            }
        )).mapSuccess { resp.data.copy(resources = resources) }
    }

    override suspend fun updateMemo(
        identifier: String,
        content: String?,
        resources: List<Resource>?,
        visibility: MemoVisibility?,
        tags: List<String>?,
        pinned: Boolean?
    ): ApiResponse<Memo> {
        val resp = memosApi.updateMemo(getId(identifier), UpdateMemoRequest(
            content = content,
            visibility = visibility?.let { MemosVisibility.fromMemoVisibility(it) },
            pinned = pinned,
            updateTime = Date(),
        )).mapSuccess { convertMemo(this) }
        if (resp !is ApiResponse.Success || resources == null || resources.map { it.identifier }.toSet() == resp.data.resources.map { it.identifier }.toSet()) {
            return resp
        }
        return memosApi.setMemoResources(getId(identifier), MemosV1SetMemoResourcesRequest(
            resources.map {
                MemosV1SetMemoResourcesRequestItem(getName(it.identifier))
            }
        )).mapSuccess { resp.data.copy(resources = resources) }
    }

    override suspend fun deleteMemo(identifier: String): ApiResponse<Unit> {
        return memosApi.deleteMemo(getId(identifier))
    }

    override suspend fun archiveMemo(identifier: String): ApiResponse<Unit> {
        return memosApi.updateMemo(getId(identifier), UpdateMemoRequest(state = MemosV1State.ARCHIVED)).mapSuccess {  }
    }

    override suspend fun restoreMemo(identifier: String): ApiResponse<Unit> {
        return memosApi.updateMemo(getId(identifier), UpdateMemoRequest(state = MemosV1State.NORMAL)).mapSuccess {  }
    }

    override suspend fun listTags(): ApiResponse<List<String>> {
        return memosApi.getUserStats(account.info.id.toString()).mapSuccess {
            tagCount.keys.toList()
        }
    }

    override suspend fun deleteTag(name: String): ApiResponse<Unit> {
        return memosApi.deleteMemoTag("-", name, false)
    }

    override suspend fun listResources(): ApiResponse<List<Resource>> {
        return memosApi.listResources().mapSuccess { this.attachments.map { convertResource(it) } }
    }

    override suspend fun createResource(
        filename: String,
        type: MediaType?,
        content: ByteArray,
        memoIdentifier: String?
    ): ApiResponse<Resource> {
        return memosApi.createResource(CreateResourceRequest(
            filename = filename,
            type = type?.toString() ?: "application/octet-stream",
            content = content.toByteString().base64(),
            memo = memoIdentifier?.let { getName(it) }
        )).mapSuccess { convertResource(this) }
    }

    override suspend fun deleteResource(identifier: String): ApiResponse<Unit> {
        return memosApi.deleteResource(getId(identifier))
    }

    override suspend fun getCurrentUser(): ApiResponse<User> {
        val resp = memosApi.authStatus().mapSuccess {
            if (user == null) {
                throw MoeMemosException.notLogin
            }
            User(user.name, user.displayName, user.createTime.toInstant())
        }
        if (resp !is ApiResponse.Success) {
            return resp
        }

        return memosApi.getUserSetting(getId(resp.data.identifier)).mapSuccess {
            resp.data.copy(
                defaultVisibility = memoVisibility.toMemoVisibility()
            )
        }
    }
}