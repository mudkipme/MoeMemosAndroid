package me.mudkip.moememos.data.repository

import android.net.Uri
import androidx.core.net.toUri
import com.skydoves.sandwich.ApiResponse
import me.mudkip.moememos.data.local.FileStorage
import me.mudkip.moememos.data.local.dao.MemoDao
import me.mudkip.moememos.data.local.entity.MemoEntity
import me.mudkip.moememos.data.local.entity.ResourceEntity
import me.mudkip.moememos.data.model.Account
import me.mudkip.moememos.data.model.Memo
import me.mudkip.moememos.data.model.MemoVisibility
import me.mudkip.moememos.data.model.Resource
import me.mudkip.moememos.data.model.User
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import java.time.Instant
import java.util.UUID

class LocalDatabaseRepository(
    private val memoDao: MemoDao,
    private val fileStorage: FileStorage,
) : AbstractMemoRepository() {
    private val accountKey = Account.Local.accountKey()

    override suspend fun listMemos(): ApiResponse<List<Memo>> {
        return try {
            val memos = memoDao.getAllMemos(accountKey).map { convertToMemo(it) }
            ApiResponse.Success(memos)
        } catch (e: Exception) {
            ApiResponse.Failure.Exception(e)
        }
    }

    override suspend fun listArchivedMemos(): ApiResponse<List<Memo>> {
        return try {
            val memos = memoDao.getArchivedMemos(accountKey).map { convertToMemo(it) }
            ApiResponse.Success(memos)
        } catch (e: Exception) {
            ApiResponse.Failure.Exception(e)
        }
    }

    override suspend fun createMemo(
        content: String,
        visibility: MemoVisibility,
        resources: List<Resource>,
        tags: List<String>?
    ): ApiResponse<Memo> {
        return try {
            val user = when (val userResponse = getCurrentUser()) {
                is ApiResponse.Success -> userResponse.data
                else -> User("local", "Local User")
            }

            val now = Instant.now()
            val memo = MemoEntity(
                identifier = UUID.randomUUID().toString(),
                accountKey = accountKey,
                content = content,
                date = now,
                visibility = visibility,
                creatorId = user.identifier,
                creatorName = user.name,
                pinned = false,
                archived = false,
                needsSync = false,
                isDeleted = false,
                lastModified = now,
                lastSyncedAt = now
            )
            memoDao.insertMemo(memo)

            resources.forEach { resource ->
                memoDao.insertResource(convertToResourceEntity(resource, memo.identifier))
            }

            ApiResponse.Success(convertToMemo(memo))
        } catch (e: Exception) {
            ApiResponse.Failure.Exception(e)
        }
    }

    override suspend fun updateMemo(
        identifier: String,
        content: String?,
        resources: List<Resource>?,
        visibility: MemoVisibility?,
        tags: List<String>?,
        pinned: Boolean?
    ): ApiResponse<Memo> {
        return try {
            val existingMemo = memoDao.getMemoById(identifier, accountKey)
                ?: return ApiResponse.Failure.Exception(Exception("Memo not found"))

            val updatedAt = Instant.now()
            val updatedMemo = existingMemo.copy(
                content = content ?: existingMemo.content,
                visibility = visibility ?: existingMemo.visibility,
                pinned = pinned ?: existingMemo.pinned,
                lastModified = updatedAt,
                lastSyncedAt = updatedAt,
                needsSync = false,
                isDeleted = false
            )
            memoDao.insertMemo(updatedMemo)

            if (resources != null) {
                memoDao.getMemoResources(identifier, accountKey).forEach {
                    memoDao.deleteResource(it)
                }
                resources.forEach { resource ->
                    memoDao.insertResource(convertToResourceEntity(resource, identifier))
                }
            }

            ApiResponse.Success(convertToMemo(updatedMemo))
        } catch (e: Exception) {
            ApiResponse.Failure.Exception(e)
        }
    }

    override suspend fun deleteMemo(identifier: String): ApiResponse<Unit> {
        return try {
            val memo = memoDao.getMemoById(identifier, accountKey)
                ?: return ApiResponse.Failure.Exception(Exception("Memo not found"))
            memoDao.getMemoResources(identifier, accountKey).forEach { resource ->
                deleteLocalFile(resource)
                memoDao.deleteResource(resource)
            }
            memoDao.deleteMemo(memo)
            ApiResponse.Success(Unit)
        } catch (e: Exception) {
            ApiResponse.Failure.Exception(e)
        }
    }

    override suspend fun archiveMemo(identifier: String): ApiResponse<Unit> {
        return try {
            val memo = memoDao.getMemoById(identifier, accountKey)
                ?: return ApiResponse.Failure.Exception(Exception("Memo not found"))
            val now = Instant.now()
            memoDao.insertMemo(
                memo.copy(
                    archived = true,
                    needsSync = false,
                    lastModified = now,
                    lastSyncedAt = now
                )
            )
            ApiResponse.Success(Unit)
        } catch (e: Exception) {
            ApiResponse.Failure.Exception(e)
        }
    }

    override suspend fun restoreMemo(identifier: String): ApiResponse<Unit> {
        return try {
            val memo = memoDao.getMemoById(identifier, accountKey)
                ?: return ApiResponse.Failure.Exception(Exception("Memo not found"))
            val now = Instant.now()
            memoDao.insertMemo(
                memo.copy(
                    archived = false,
                    needsSync = false,
                    lastModified = now,
                    lastSyncedAt = now
                )
            )
            ApiResponse.Success(Unit)
        } catch (e: Exception) {
            ApiResponse.Failure.Exception(e)
        }
    }

    override suspend fun listTags(): ApiResponse<List<String>> {
        return try {
            val tags = memoDao.getAllMemos(accountKey)
                .asSequence()
                .flatMap { TAG_REGEX.findAll(it.content).map { match -> match.groupValues[1] } }
                .filter { it.isNotBlank() }
                .toSet()
                .sorted()
            ApiResponse.Success(tags)
        } catch (e: Exception) {
            ApiResponse.Failure.Exception(e)
        }
    }

    override suspend fun listResources(): ApiResponse<List<Resource>> {
        return try {
            val resources = memoDao.getAllResources(accountKey)
            ApiResponse.Success(resources.map { convertToResource(it) })
        } catch (e: Exception) {
            ApiResponse.Failure.Exception(e)
        }
    }

    override suspend fun createResource(
        filename: String,
        type: MediaType?,
        content: ByteArray,
        memoIdentifier: String?
    ): ApiResponse<Resource> {
        return try {
            val uri = fileStorage.saveFile(
                accountKey = accountKey,
                content = content,
                filename = UUID.randomUUID().toString() + "_" + filename
            )
            val resource = ResourceEntity(
                identifier = UUID.randomUUID().toString(),
                remoteId = null,
                accountKey = accountKey,
                date = Instant.now(),
                filename = filename,
                uri = uri.toString(),
                localUri = uri.toString(),
                mimeType = type?.toString(),
                memoId = memoIdentifier ?: ""
            )
            memoDao.insertResource(resource)
            ApiResponse.Success(convertToResource(resource))
        } catch (e: Exception) {
            ApiResponse.Failure.Exception(e)
        }
    }

    override suspend fun deleteResource(identifier: String): ApiResponse<Unit> {
        return try {
            val resource = memoDao.getResourceById(identifier, accountKey)
                ?: return ApiResponse.Failure.Exception(Exception("Resource not found"))
            deleteLocalFile(resource)
            memoDao.deleteResource(resource)
            ApiResponse.Success(Unit)
        } catch (e: Exception) {
            ApiResponse.Failure.Exception(e)
        }
    }

    override suspend fun getCurrentUser(): ApiResponse<User> {
        return ApiResponse.Success(User("local", "Local Account"))
    }

    private suspend fun convertToMemo(entity: MemoEntity): Memo {
        val resources = memoDao.getMemoResources(entity.identifier, accountKey).map { convertToResource(it) }
        return Memo(
            identifier = entity.identifier,
            remoteId = entity.remoteId,
            content = entity.content,
            date = entity.date,
            pinned = entity.pinned,
            visibility = entity.visibility,
            resources = resources,
            tags = emptyList(),
            creator = if (entity.creatorId != null && entity.creatorName != null) {
                User(entity.creatorId, entity.creatorName)
            } else {
                null
            },
            archived = entity.archived,
            updatedAt = entity.lastModified,
            needsSync = false
        )
    }

    private fun convertToResource(entity: ResourceEntity): Resource {
        return Resource(
            identifier = entity.identifier,
            remoteId = entity.remoteId,
            date = entity.date,
            filename = entity.filename,
            mimeType = entity.mimeType?.toMediaTypeOrNull(),
            uri = entity.uri.toUri(),
            localUri = entity.localUri?.toUri()
        )
    }

    private fun convertToResourceEntity(resource: Resource, memoId: String): ResourceEntity {
        return ResourceEntity(
            identifier = resource.identifier,
            remoteId = resource.remoteId,
            accountKey = accountKey,
            date = resource.date,
            filename = resource.filename,
            mimeType = resource.mimeType?.toString(),
            uri = resource.uri.toString(),
            localUri = resource.localUri?.toString() ?: resource.uri.toString(),
            memoId = memoId
        )
    }

    private fun deleteLocalFile(resource: ResourceEntity) {
        val local = resource.localUri ?: resource.uri
        val localUri = local.toUri()
        if (localUri.scheme == "file") {
            fileStorage.deleteFile(localUri)
        }
    }

    companion object {
        private val TAG_REGEX = Regex("(?:^|\\s)#([\\w/-]+)")
    }
}
