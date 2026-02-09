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
import okio.ByteString.Companion.toByteString
import java.time.Instant

private const val PAGE_SIZE = 200

class MemosV1Repository(
    private val memosApi: MemosV1Api,
    private val account: Account.MemosV1
): RemoteRepository() {
    private fun convertResource(resource: MemosV1Resource): Resource {
        return Resource(
            remoteId = requireNotNull(resource.name),
            date = resource.createTime ?: Instant.now(),
            filename = resource.filename ?: "",
            uri = resource.uri(account.info.host).toString(),
            mimeType = resource.type
        )
    }

    private fun convertMemo(memo: MemosV1Memo): Memo {
        return Memo(
            remoteId = memo.name,
            content = memo.content ?: "",
            date = memo.displayTime ?: Instant.now(),
            pinned = memo.pinned ?: false,
            visibility = memo.visibility?.toMemoVisibility() ?: MemoVisibility.PRIVATE,
            resources = memo.attachments?.map { convertResource(it) } ?: emptyList(),
            tags = emptyList(),
            archived = memo.state == MemosV1State.ARCHIVED,
            updatedAt = memo.updateTime
        )
    }

    private suspend fun listMemosByFilter(state: MemosV1State, filter: String): ApiResponse<List<Memo>> {
        var nextPageToken = ""
        val memos = arrayListOf<Memo>()

        do {
            val resp = memosApi.listMemos(PAGE_SIZE, nextPageToken, state, filter)
                .onSuccess { nextPageToken = data.nextPageToken.orEmpty() }
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
        return listMemosByFilter(MemosV1State.NORMAL, "creator_id == ${account.info.id}")
    }

    override suspend fun listArchivedMemos(): ApiResponse<List<Memo>> {
        return listMemosByFilter(MemosV1State.ARCHIVED, "creator_id == ${account.info.id}")
    }

    override suspend fun listWorkspaceMemos(
        pageSize: Int,
        pageToken: String?
    ): ApiResponse<Pair<List<Memo>, String?>> {
        val resp = memosApi.listMemos(pageSize, pageToken)
        if (resp !is ApiResponse.Success) {
            return resp.mapSuccess { emptyList<Memo>() to null }
        }
        val users = resp.data.memos.mapNotNull { it.creator }.map { getId(it) }.toSet()
        val userResp = coroutineScope {
            users.map { userId ->
                async { memosApi.getUser(userId).getOrNull() }
            }.awaitAll()
        }.filterNotNull()
        val userMap = mapOf(*userResp.map { user -> user.name to user }.toTypedArray())

        return resp
            .mapSuccess { this.memos.map {
                convertMemo(it).copy(
                    creator = it.creator?.let { creator ->
                        userMap[creator]?.let { user ->
                            User(
                                user.name,
                                user.displayName ?: user.username,
                                user.createTime ?: Instant.now()
                            )
                        }
                    }
                )
            } to this.nextPageToken?.ifEmpty { null } }
    }

    override suspend fun createMemo(
        content: String,
        visibility: MemoVisibility,
        resourceRemoteIds: List<String>,
        tags: List<String>?
    ): ApiResponse<Memo> {
        val resp = memosApi.createMemo(MemosV1CreateMemoRequest(content, MemosVisibility.fromMemoVisibility(visibility), resourceRemoteIds.map { MemosV1Resource(name = getName(it)) }))
            .mapSuccess { convertMemo(this) }
        return resp
    }

    override suspend fun updateMemo(
        remoteId: String,
        content: String?,
        resourceRemoteIds: List<String>?,
        visibility: MemoVisibility?,
        tags: List<String>?,
        pinned: Boolean?,
        archived: Boolean?
    ): ApiResponse<Memo> {
        val resp = memosApi.updateMemo(getId(remoteId), UpdateMemoRequest(
            content = content,
            visibility = visibility?.let { MemosVisibility.fromMemoVisibility(it) },
            pinned = pinned,
            state = archived?.let { isArchived -> if (isArchived) MemosV1State.ARCHIVED else MemosV1State.NORMAL },
            updateTime = Instant.now(),
            attachments = resourceRemoteIds?.map { MemosV1Resource(name = getName(it)) }
        )).mapSuccess { convertMemo(this) }
        return resp
    }

    override suspend fun deleteMemo(remoteId: String): ApiResponse<Unit> {
        return memosApi.deleteMemo(getId(remoteId))
    }

    override suspend fun listTags(): ApiResponse<List<String>> {
        return memosApi.getUserStats(account.info.id.toString()).mapSuccess {
            tagCount.keys.toList()
          }
    }

    override suspend fun listResources(): ApiResponse<List<Resource>> {
        return memosApi.listResources().mapSuccess { this.attachments.map { convertResource(it) } }
    }

    override suspend fun createResource(
        filename: String,
        type: MediaType?,
        content: ByteArray,
        memoRemoteId: String?
    ): ApiResponse<Resource> {
        return memosApi.createResource(CreateResourceRequest(
            filename = filename,
            type = type?.toString() ?: "application/octet-stream",
            content = content.toByteString().base64(),
            memo = memoRemoteId?.let { getName(it) }
        )).mapSuccess { convertResource(this) }
    }

    override suspend fun deleteResource(remoteId: String): ApiResponse<Unit> {
        return memosApi.deleteResource(getId(remoteId))
    }

    override suspend fun getCurrentUser(): ApiResponse<User> {
        val resp = memosApi.getCurrentUser().mapSuccess {
            if (user == null) {
                throw MoeMemosException.notLogin
            }
            User(
                user.name,
                user.displayName ?: user.username,
                user.createTime ?: Instant.now()
            )
        }
        if (resp !is ApiResponse.Success) {
            return resp
        }

        return memosApi.getUserSetting(getId(resp.data.identifier)).mapSuccess {
            resp.data.copy(
                defaultVisibility = generalSetting?.memoVisibility?.toMemoVisibility() ?: MemoVisibility.PRIVATE
            )
        }
    }
}
