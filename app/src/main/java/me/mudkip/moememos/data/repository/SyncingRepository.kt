package me.mudkip.moememos.data.repository

import android.net.Uri
import androidx.core.net.toUri
import com.skydoves.sandwich.ApiResponse
import com.skydoves.sandwich.getOrNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
import java.io.File
import java.time.Instant
import java.util.UUID

class SyncingRepository(
    private val memoDao: MemoDao,
    private val fileStorage: FileStorage,
    private val remoteRepository: RemoteRepository,
    private val account: Account,
) : AbstractMemoRepository() {
    private val accountKey = account.accountKey()
    private val operationMutex = Mutex()
    private val operationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

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
            val memos = memoDao.getArchivedMemos(accountKey)
                .filterNot { it.isDeleted }
                .map { convertToMemo(it) }
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
            val now = Instant.now()
            val localMemo = MemoEntity(
                identifier = UUID.randomUUID().toString(),
                remoteId = null,
                accountKey = accountKey,
                content = content,
                date = now,
                visibility = visibility,
                creatorId = account.getAccountInfo()?.id?.toString(),
                creatorName = account.getAccountInfo()?.name,
                pinned = false,
                archived = false,
                needsSync = true,
                isDeleted = false,
                lastModified = now,
                lastSyncedAt = null
            )
            memoDao.insertMemo(localMemo)

            resources.forEach { resource ->
                memoDao.insertResource(
                    ResourceEntity(
                        identifier = resource.identifier,
                        remoteId = resource.remoteId,
                        accountKey = accountKey,
                        date = resource.date,
                        filename = resource.filename,
                        uri = resource.uri.toString(),
                        localUri = resource.localUri?.toString(),
                        mimeType = resource.mimeType?.toString(),
                        memoId = localMemo.identifier
                    )
                )
            }

            enqueuePushMemo(localMemo.identifier)
            ApiResponse.Success(convertToMemo(localMemo))
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

            val updatedMemo = existingMemo.copy(
                content = content ?: existingMemo.content,
                visibility = visibility ?: existingMemo.visibility,
                pinned = pinned ?: existingMemo.pinned,
                needsSync = true,
                isDeleted = false,
                lastModified = Instant.now()
            )
            memoDao.insertMemo(updatedMemo)

            if (resources != null) {
                memoDao.getMemoResources(identifier, accountKey).forEach {
                    memoDao.deleteResource(it)
                }
                resources.forEach { resource ->
                    memoDao.insertResource(
                        ResourceEntity(
                            identifier = resource.identifier,
                            remoteId = resource.remoteId,
                            accountKey = accountKey,
                            date = resource.date,
                            filename = resource.filename,
                            mimeType = resource.mimeType?.toString(),
                            uri = resource.uri.toString(),
                            localUri = resource.localUri?.toString(),
                            memoId = identifier
                        )
                    )
                }
            }

            enqueuePushMemo(updatedMemo.identifier)
            ApiResponse.Success(convertToMemo(updatedMemo))
        } catch (e: Exception) {
            ApiResponse.Failure.Exception(e)
        }
    }

    override suspend fun deleteMemo(identifier: String): ApiResponse<Unit> {
        return try {
            val memo = memoDao.getMemoById(identifier, accountKey)
                ?: return ApiResponse.Failure.Exception(Exception("Memo not found"))
            memoDao.insertMemo(
                memo.copy(
                    isDeleted = true,
                    needsSync = true,
                    lastModified = Instant.now()
                )
            )
            enqueuePushMemo(identifier)
            ApiResponse.Success(Unit)
        } catch (e: Exception) {
            ApiResponse.Failure.Exception(e)
        }
    }

    override suspend fun archiveMemo(identifier: String): ApiResponse<Unit> {
        return try {
            val memo = memoDao.getMemoById(identifier, accountKey)
                ?: return ApiResponse.Failure.Exception(Exception("Memo not found"))
            memoDao.insertMemo(
                memo.copy(
                    archived = true,
                    needsSync = true,
                    lastModified = Instant.now()
                )
            )
            enqueuePushMemo(identifier)
            ApiResponse.Success(Unit)
        } catch (e: Exception) {
            ApiResponse.Failure.Exception(e)
        }
    }

    override suspend fun restoreMemo(identifier: String): ApiResponse<Unit> {
        return try {
            val memo = memoDao.getMemoById(identifier, accountKey)
                ?: return ApiResponse.Failure.Exception(Exception("Memo not found"))
            memoDao.insertMemo(
                memo.copy(
                    archived = false,
                    needsSync = true,
                    lastModified = Instant.now()
                )
            )
            enqueuePushMemo(identifier)
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
            val resources = memoDao.getAllResources(accountKey).map { convertToResource(it) }
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
            if (!memoIdentifier.isNullOrBlank()) {
                memoDao.getMemoById(memoIdentifier, accountKey)?.let { memo ->
                    memoDao.insertMemo(
                        memo.copy(
                            needsSync = true,
                            lastModified = Instant.now()
                        )
                    )
                }
                enqueuePushMemo(memoIdentifier)
            } else {
                enqueuePushResource(resource.identifier)
            }
            ApiResponse.Success(convertToResource(resource))
        } catch (e: Exception) {
            ApiResponse.Failure.Exception(e)
        }
    }

    override suspend fun deleteResource(identifier: String): ApiResponse<Unit> {
        return try {
            val resource = memoDao.getResourceById(identifier, accountKey)
                ?: return ApiResponse.Failure.Exception(Exception("Resource not found"))

            val memoId = resource.memoId
            deleteLocalFile(resource)
            memoDao.deleteResource(resource)

            if (!memoId.isNullOrBlank()) {
                memoDao.getMemoById(memoId, accountKey)?.let { memo ->
                    memoDao.insertMemo(
                        memo.copy(
                            needsSync = true,
                            lastModified = Instant.now()
                        )
                    )
                }
            }

            resource.remoteId?.let { remoteId ->
                enqueueDeleteRemoteResource(remoteId)
            }
            if (!memoId.isNullOrBlank()) {
                enqueuePushMemo(memoId)
            }
            ApiResponse.Success(Unit)
        } catch (e: Exception) {
            ApiResponse.Failure.Exception(e)
        }
    }

    override suspend fun cacheResourceFile(identifier: String, downloadedUri: Uri): ApiResponse<Unit> {
        return try {
            val resource = memoDao.getResourceById(identifier, accountKey)
                ?: return ApiResponse.Failure.Exception(Exception("Resource not found"))
            val existingLocal = existingLocalUri(resource)
            if (existingLocal != null) {
                return ApiResponse.Success(Unit)
            }

            val sourcePath = downloadedUri.path ?: return ApiResponse.Failure.Exception(Exception("Invalid downloaded file"))
            val sourceFile = File(sourcePath)
            if (!sourceFile.exists()) {
                return ApiResponse.Failure.Exception(Exception("Downloaded file does not exist"))
            }

            val canonical = fileStorage.saveFile(
                accountKey = accountKey,
                content = sourceFile.readBytes(),
                filename = "${resource.identifier}_${resource.filename}"
            ).toString()

            resource.localUri?.takeIf { it != canonical }?.let { oldLocal ->
                val oldUri = oldLocal.toUri()
                if (oldUri.scheme == "file") {
                    fileStorage.deleteFile(oldUri)
                }
            }

            val updatedUri = if (resource.remoteId == null && resource.uri.toUri().scheme == "file") {
                canonical
            } else {
                resource.uri
            }
            memoDao.insertResource(
                resource.copy(
                    uri = updatedUri,
                    localUri = canonical
                )
            )
            ApiResponse.Success(Unit)
        } catch (e: Exception) {
            ApiResponse.Failure.Exception(e)
        }
    }

    override suspend fun getCurrentUser(): ApiResponse<User> {
        val remoteUser = remoteRepository.getCurrentUser()
        if (remoteUser is ApiResponse.Success) {
            return remoteUser
        }

        val info = account.getAccountInfo()
        return if (info != null) {
            ApiResponse.Success(User(identifier = info.id.toString(), name = info.name))
        } else {
            remoteUser
        }
    }

    override suspend fun sync(): ApiResponse<Unit> {
        return operationMutex.withLock {
            try {
                syncInternal()
            } catch (e: Throwable) {
                ApiResponse.Failure.Exception(e)
            }
        }
    }

    private suspend fun syncInternal(): ApiResponse<Unit> {
        val remoteNormal = remoteRepository.listMemos()
        if (remoteNormal !is ApiResponse.Success) {
            return remoteNormal.mapFailureToUnit()
        }

        val remoteArchived = remoteRepository.listArchivedMemos()
        if (remoteArchived !is ApiResponse.Success) {
            return remoteArchived.mapFailureToUnit()
        }

        val remoteMemos = remoteNormal.data + remoteArchived.data
        val remoteById = remoteMemos.associateBy { remoteMemoId(it) }

        var hadErrors = false
        val localMemos = memoDao.getAllMemosForSync(accountKey)
        val localByRemoteId = localMemos.mapNotNull { memo ->
            memo.remoteId?.let { it to memo }
        }.toMap()

        for (remoteMemo in remoteMemos) {
            val remoteId = remoteMemoId(remoteMemo)
            val local = localByRemoteId[remoteId]

            if (local == null) {
                applyRemoteMemo(remoteMemo)
                continue
            }

            if (local.isDeleted) {
                if (!local.needsSync) {
                    applyRemoteMemo(remoteMemo, local.identifier)
                    continue
                }

                val remoteChanged = hasRemoteChanged(local, remoteMemo)
                val equivalent = memoEquivalent(local, remoteMemo)
                if (remoteChanged || !equivalent) {
                    applyRemoteMemo(remoteMemo, local.identifier)
                } else {
                    val deleted = remoteRepository.deleteMemo(remoteId)
                    if (deleted is ApiResponse.Success) {
                        permanentlyDeleteMemo(local.identifier)
                    } else {
                        hadErrors = true
                    }
                }
                continue
            }

            val equivalent = memoEquivalent(local, remoteMemo)
            if (equivalent) {
                markSynced(local, remoteMemo)
                continue
            }

            val localChanged = local.needsSync
            val remoteChanged = hasRemoteChanged(local, remoteMemo)

            when {
                !localChanged -> applyRemoteMemo(remoteMemo, local.identifier)
                !remoteChanged -> {
                    if (!pushLocalMemo(local.identifier)) {
                        hadErrors = true
                    }
                }
                else -> {
                    if (!duplicateConflict(local, remoteMemo)) {
                        hadErrors = true
                    }
                }
            }
        }

        val latestLocals = memoDao.getAllMemosForSync(accountKey)
        for (local in latestLocals) {
            if (local.remoteId != null && remoteById.containsKey(local.remoteId)) {
                continue
            }

            if (local.remoteId != null && !remoteById.containsKey(local.remoteId)) {
                if (local.isDeleted) {
                    permanentlyDeleteMemo(local.identifier)
                } else if (local.needsSync) {
                    if (!pushLocalMemo(local.identifier, forceCreate = true)) {
                        hadErrors = true
                    }
                } else {
                    permanentlyDeleteMemo(local.identifier)
                }
                continue
            }

            if (local.remoteId == null) {
                if (local.isDeleted) {
                    permanentlyDeleteMemo(local.identifier)
                } else if (local.needsSync) {
                    if (!pushLocalMemo(local.identifier, forceCreate = true)) {
                        hadErrors = true
                    }
                }
            }
        }

        return if (hadErrors) {
            ApiResponse.Failure.Exception(Exception("Sync finished with partial failures"))
        } else {
            ApiResponse.Success(Unit)
        }
    }

    private suspend fun pushLocalMemo(identifier: String, forceCreate: Boolean = false): Boolean {
        val local = memoDao.getMemoById(identifier, accountKey) ?: return true

        if (local.isDeleted) {
            return if (local.remoteId != null) {
                val deleted = remoteRepository.deleteMemo(local.remoteId)
                if (deleted is ApiResponse.Success) {
                    permanentlyDeleteMemo(local.identifier)
                    true
                } else {
                    false
                }
            } else {
                permanentlyDeleteMemo(local.identifier)
                true
            }
        }

        val remoteResourceIds = ensureUploadedResources(local)

        return if (!forceCreate && local.remoteId != null) {
            val updated = remoteRepository.updateMemo(
                remoteId = local.remoteId,
                content = local.content,
                resourceRemoteIds = remoteResourceIds,
                visibility = local.visibility,
                pinned = local.pinned
            )
            if (updated is ApiResponse.Success) {
                val stateSynced = if (local.archived) {
                    remoteRepository.archiveMemo(local.remoteId)
                } else {
                    remoteRepository.restoreMemo(local.remoteId)
                }
                if (stateSynced !is ApiResponse.Success) {
                    return false
                }
                reconcileServerCreatedMemo(
                    local.identifier,
                    updated.data.copy(archived = local.archived)
                )
                true
            } else {
                false
            }
        } else {
            val created = remoteRepository.createMemo(
                content = local.content,
                visibility = local.visibility,
                resourceRemoteIds = remoteResourceIds,
                tags = null
            )
            if (created !is ApiResponse.Success) {
                return false
            }

            val createdRemoteId = remoteMemoId(created.data)

            reconcileServerCreatedMemo(
                local.identifier,
                created.data.copy(
                    remoteId = createdRemoteId,
                )
            )
            true
        }
    }

    private suspend fun duplicateConflict(local: MemoEntity, remoteMemo: Memo): Boolean {
        val duplicateLocal = local.copy(
            identifier = UUID.randomUUID().toString(),
            remoteId = null,
            needsSync = true,
            isDeleted = false,
            lastSyncedAt = null,
            lastModified = Instant.now()
        )

        memoDao.insertMemo(duplicateLocal)
        memoDao.getMemoResources(local.identifier, accountKey).forEach { resource ->
            memoDao.insertResource(
                resource.copy(
                    identifier = UUID.randomUUID().toString(),
                    memoId = duplicateLocal.identifier
                )
            )
        }

        applyRemoteMemo(remoteMemo, local.identifier)
        return pushLocalMemo(duplicateLocal.identifier, forceCreate = true)
    }

    private suspend fun reconcileServerCreatedMemo(localIdentifier: String, remoteMemo: Memo) {
        applyRemoteMemo(remoteMemo, preferredLocalIdentifier = localIdentifier)
    }

    private suspend fun ensureUploadedResources(localMemo: MemoEntity): List<String> {
        val resources = memoDao.getMemoResources(localMemo.identifier, accountKey)
        val uploaded = arrayListOf<String>()

        for (resource in resources) {
            val ensured = ensureUploadedResource(resource, localMemo.remoteId)
            if (ensured?.remoteId != null) {
                uploaded.add(ensured.remoteId)
            }
        }

        return uploaded
    }

    private suspend fun ensureUploadedResource(
        resource: ResourceEntity,
        memoRemoteId: String?
    ): ResourceEntity? {
        if (resource.remoteId != null) {
            return resource
        }

        val uri = (resource.localUri ?: resource.uri).toUri()
        if (uri.scheme != "file") {
            return null
        }

        val path = uri.path ?: return null
        val file = File(path)
        if (!file.exists()) {
            return null
        }

        val uploaded = remoteRepository.createResource(
            filename = resource.filename,
            type = resource.mimeType?.toMediaTypeOrNull(),
            content = file.readBytes(),
            memoRemoteId = memoRemoteId
        )

        val remoteResource = uploaded.getOrNull() ?: return null
        val synced = resource.copy(
            remoteId = remoteResourceId(remoteResource),
            uri = remoteResource.uri.toString(),
            localUri = resource.localUri ?: resource.uri
        )
        memoDao.insertResource(synced)
        return synced
    }

    private suspend fun applyRemoteMemo(
        remoteMemo: Memo,
        preferredLocalIdentifier: String? = null
    ) {
        val remoteId = remoteMemoId(remoteMemo)
        val current = memoDao.getMemoByRemoteId(remoteId, accountKey)
            ?: preferredLocalIdentifier?.let { memoDao.getMemoById(it, accountKey) }

        val localIdentifier = current?.identifier ?: UUID.randomUUID().toString()
        val remoteUpdatedAt = remoteMemo.updatedAt ?: remoteMemo.date

        memoDao.insertMemo(
            MemoEntity(
                identifier = localIdentifier,
                remoteId = remoteId,
                accountKey = accountKey,
                content = remoteMemo.content,
                date = remoteMemo.date,
                visibility = remoteMemo.visibility,
                creatorId = remoteMemo.creator?.identifier,
                creatorName = remoteMemo.creator?.name,
                pinned = remoteMemo.pinned,
                archived = remoteMemo.archived,
                needsSync = false,
                isDeleted = false,
                lastModified = remoteUpdatedAt,
                lastSyncedAt = remoteUpdatedAt
            )
        )

        val currentResources = memoDao.getMemoResources(localIdentifier, accountKey)
        currentResources.forEach { memoDao.deleteResource(it) }

        remoteMemo.resources.forEach { resource ->
            val remoteResourceId = remoteResourceId(resource)
            val existing = currentResources.firstOrNull { it.remoteId == remoteResourceId }
            val localResourceIdentifier = existing?.identifier ?: UUID.randomUUID().toString()
            val preferredLocalUri = when {
                existing?.localUri != null && File(existing.localUri.toUri().path ?: "").exists() -> existing.localUri
                existing != null && existing.uri.toUri().scheme == "file" && File(existing.uri.toUri().path ?: "").exists() -> existing.uri
                else -> null
            }
            memoDao.insertResource(
                ResourceEntity(
                    identifier = localResourceIdentifier,
                    remoteId = remoteResourceId,
                    accountKey = accountKey,
                    date = resource.date,
                    filename = resource.filename,
                    uri = resource.uri.toString(),
                    localUri = preferredLocalUri,
                    mimeType = resource.mimeType?.toString(),
                    memoId = localIdentifier
                )
            )
        }
    }

    private suspend fun markSynced(local: MemoEntity, remoteMemo: Memo) {
        memoDao.insertMemo(
            local.copy(
                remoteId = remoteMemoId(remoteMemo),
                needsSync = false,
                isDeleted = false,
                archived = remoteMemo.archived,
                lastSyncedAt = remoteMemo.updatedAt ?: remoteMemo.date
            )
        )
    }

    private suspend fun memoEquivalent(local: MemoEntity, remote: Memo): Boolean {
        if (local.content != remote.content) {
            return false
        }
        if (local.pinned != remote.pinned) {
            return false
        }
        if (local.visibility != remote.visibility) {
            return false
        }
        if (local.archived != remote.archived) {
            return false
        }

        val localResources = memoDao.getMemoResources(local.identifier, accountKey)
        return resourceEntitySignature(localResources) == resourceModelSignature(remote.resources)
    }

    private fun hasRemoteChanged(local: MemoEntity, remote: Memo): Boolean {
        val remoteUpdatedAt = remote.updatedAt ?: remote.date
        val lastSyncedAt = local.lastSyncedAt ?: return true
        return remoteUpdatedAt != lastSyncedAt
    }

    private fun resourceEntitySignature(resources: List<ResourceEntity>): List<String> {
        return resources.map { resource ->
            resource.remoteId ?: "local:${resource.localUri ?: resource.uri}"
        }.sorted()
    }

    private fun resourceModelSignature(resources: List<Resource>): List<String> {
        return resources.map { resource ->
            resource.remoteId ?: resource.identifier
        }.sorted()
    }

    private suspend fun pushLocalResource(identifier: String): Boolean {
        val local = memoDao.getResourceById(identifier, accountKey) ?: return true
        val ensured = ensureUploadedResource(local, memoRemoteId = null) ?: return false
        return ensured.remoteId != null
    }

    private fun enqueuePushMemo(identifier: String, forceCreate: Boolean = false) {
        operationScope.launch {
            operationMutex.withLock {
                try {
                    pushLocalMemo(identifier, forceCreate)
                } catch (_: Throwable) {
                }
            }
        }
    }

    private fun enqueuePushResource(identifier: String) {
        operationScope.launch {
            operationMutex.withLock {
                try {
                    pushLocalResource(identifier)
                } catch (_: Throwable) {
                }
            }
        }
    }

    private fun enqueueDeleteRemoteResource(remoteId: String) {
        operationScope.launch {
            operationMutex.withLock {
                try {
                    remoteRepository.deleteResource(remoteId)
                } catch (_: Throwable) {
                }
            }
        }
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
            updatedAt = entity.lastModified
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

    private suspend fun permanentlyDeleteMemo(identifier: String) {
        memoDao.getMemoById(identifier, accountKey)?.let { memo ->
            memoDao.getMemoResources(identifier, accountKey).forEach { resource ->
                deleteLocalFile(resource)
            }
            memoDao.deleteMemo(memo)
        }
    }

    private fun existingLocalUri(resource: ResourceEntity): Uri? {
        val local = resource.localUri ?: return null
        val uri = local.toUri()
        return if (uri.scheme == "file" && File(uri.path ?: "").exists()) uri else null
    }

    private fun deleteLocalFile(resource: ResourceEntity) {
        val uri = resource.localUri?.toUri()
            ?: resource.uri.toUri().takeIf { it.scheme == "file" }
        if (uri != null) {
            fileStorage.deleteFile(uri)
        }
    }

    private fun remoteMemoId(memo: Memo): String {
        return memo.remoteId?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("RemoteRepository must return memos with non-empty remoteId")
    }

    private fun remoteResourceId(resource: Resource): String {
        return resource.remoteId?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("RemoteRepository must return resources with non-empty remoteId")
    }

    private fun <T> ApiResponse<T>.mapFailureToUnit(): ApiResponse<Unit> {
        return when (this) {
            is ApiResponse.Success -> ApiResponse.Success(Unit)
            is ApiResponse.Failure.Error -> ApiResponse.Failure.Error(this.payload)
            is ApiResponse.Failure.Exception -> ApiResponse.Failure.Exception(this.throwable)
        }
    }

    companion object {
        private val TAG_REGEX = Regex("(?:^|\\s)#([\\w/-]+)")
    }
}
