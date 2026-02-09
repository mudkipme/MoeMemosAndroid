package me.mudkip.moememos.data.repository

import androidx.core.net.toUri
import com.skydoves.sandwich.ApiResponse
import me.mudkip.moememos.data.local.FileStorage
import me.mudkip.moememos.data.local.dao.MemoDao
import me.mudkip.moememos.data.local.entity.MemoEntity
import me.mudkip.moememos.data.local.entity.MemoWithResources
import me.mudkip.moememos.data.local.entity.ResourceEntity
import me.mudkip.moememos.data.model.Account
import me.mudkip.moememos.data.model.MemoVisibility
import me.mudkip.moememos.data.model.User
import me.mudkip.moememos.util.extractCustomTags
import okhttp3.MediaType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.util.UUID

class LocalDatabaseRepository(
    private val memoDao: MemoDao,
    private val fileStorage: FileStorage,
    private val account: Account.Local = Account.Local(),
) : AbstractMemoRepository() {
    private val accountKey = account.accountKey()

    override fun observeMemos(): Flow<List<MemoEntity>> {
        return memoDao.observeAllMemos(accountKey).map { memos ->
            memos.map { it.toMemoEntity() }
        }
    }

    override suspend fun listMemos(): ApiResponse<List<MemoEntity>> {
        return try {
            val memos = memoDao.getAllMemos(accountKey).map { withResources(it) }
            ApiResponse.Success(memos)
        } catch (e: Exception) {
            ApiResponse.Failure.Exception(e)
        }
    }

    override suspend fun listArchivedMemos(): ApiResponse<List<MemoEntity>> {
        return try {
            val memos = memoDao.getArchivedMemos(accountKey).map { withResources(it) }
            ApiResponse.Success(memos)
        } catch (e: Exception) {
            ApiResponse.Failure.Exception(e)
        }
    }

    override suspend fun createMemo(
        content: String,
        visibility: MemoVisibility,
        resources: List<ResourceEntity>,
        tags: List<String>?
    ): ApiResponse<MemoEntity> {
        return try {
            val now = Instant.now()
            val memo = MemoEntity(
                identifier = UUID.randomUUID().toString(),
                accountKey = accountKey,
                content = content,
                date = now,
                visibility = visibility,
                pinned = false,
                archived = false,
                needsSync = false,
                isDeleted = false,
                lastModified = now,
                lastSyncedAt = now
            )
            memoDao.insertMemo(memo)

            resources.forEach { resource ->
                memoDao.insertResource(
                    resource.copy(
                        accountKey = accountKey,
                        memoId = memo.identifier
                    )
                )
            }

            ApiResponse.Success(withResources(memo))
        } catch (e: Exception) {
            ApiResponse.Failure.Exception(e)
        }
    }

    override suspend fun updateMemo(
        identifier: String,
        content: String?,
        resources: List<ResourceEntity>?,
        visibility: MemoVisibility?,
        tags: List<String>?,
        pinned: Boolean?
    ): ApiResponse<MemoEntity> {
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
                    memoDao.insertResource(
                        resource.copy(
                            accountKey = accountKey,
                            memoId = identifier
                        )
                    )
                }
            }

            ApiResponse.Success(withResources(updatedMemo))
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
                .flatMap { extractCustomTags(it.content).asSequence() }
                .filter { it.isNotBlank() }
                .toSet()
                .sorted()
            ApiResponse.Success(tags)
        } catch (e: Exception) {
            ApiResponse.Failure.Exception(e)
        }
    }

    override suspend fun listResources(): ApiResponse<List<ResourceEntity>> {
        return try {
            val resources = memoDao.getAllResources(accountKey)
            ApiResponse.Success(resources)
        } catch (e: Exception) {
            ApiResponse.Failure.Exception(e)
        }
    }

    override suspend fun createResource(
        filename: String,
        type: MediaType?,
        content: ByteArray,
        memoIdentifier: String?
    ): ApiResponse<ResourceEntity> {
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
                memoId = memoIdentifier
            )
            memoDao.insertResource(resource)
            ApiResponse.Success(resource)
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
        return ApiResponse.Success(account.toUser())
    }

    private suspend fun withResources(memo: MemoEntity): MemoEntity {
        val resources = memoDao.getMemoResources(memo.identifier, accountKey)
        return memo.copy().also { it.resources = resources }
    }

    private fun deleteLocalFile(resource: ResourceEntity) {
        val local = resource.localUri ?: resource.uri
        val localUri = local.toUri()
        if (localUri.scheme == "file") {
            fileStorage.deleteFile(localUri)
        }
    }

}

private fun MemoWithResources.toMemoEntity(): MemoEntity {
    return memo.copy().also { it.resources = resources }
}
