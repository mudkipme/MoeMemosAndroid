package me.mudkip.moememos.data.repository

import android.net.Uri
import androidx.core.net.toUri
import com.skydoves.sandwich.ApiResponse
import com.skydoves.sandwich.StatusCode
import com.skydoves.sandwich.getOrNull
import com.skydoves.sandwich.retrofit.statusCode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.mudkip.moememos.data.local.FileStorage
import me.mudkip.moememos.data.local.dao.MemoDao
import me.mudkip.moememos.data.local.entity.MemoEntity
import me.mudkip.moememos.data.local.entity.MemoWithResources
import me.mudkip.moememos.data.local.entity.ResourceEntity
import me.mudkip.moememos.data.constant.MoeMemosException
import me.mudkip.moememos.data.model.Account
import me.mudkip.moememos.data.model.Memo
import me.mudkip.moememos.data.model.MemoVisibility
import me.mudkip.moememos.data.model.Resource
import me.mudkip.moememos.data.model.SyncStatus
import me.mudkip.moememos.data.model.User
import me.mudkip.moememos.ext.getErrorMessage
import me.mudkip.moememos.util.extractCustomTags
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
    private val onUserSynced: suspend (User) -> Unit = {},
) : AbstractMemoRepository() {
    private val accountKey = account.accountKey()
    private var currentUser: User = account.toUser()
    private val operationMutex = Mutex()
    private val operationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _syncStatus = MutableStateFlow(SyncStatus())
    override val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    init {
        operationScope.launch {
            refreshUnsyncedCount()
        }
    }

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
            val memos = memoDao.getArchivedMemos(accountKey)
                .filterNot { it.isDeleted }
                .map { withResources(it) }
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
            val localMemo = MemoEntity(
                identifier = UUID.randomUUID().toString(),
                remoteId = null,
                accountKey = accountKey,
                content = content,
                date = now,
                visibility = visibility,
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
                    resource.copy(
                        accountKey = accountKey,
                        memoId = localMemo.identifier
                    )
                )
            }

            refreshUnsyncedCount()
            enqueuePushMemo(localMemo.identifier)
            ApiResponse.Success(withResources(localMemo))
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
                val existingResources = memoDao.getMemoResources(identifier, accountKey)
                val incomingIds = resources.mapTo(hashSetOf()) { it.identifier }
                existingResources.forEach { existing ->
                    if (existing.identifier !in incomingIds) {
                        deleteLocalFile(existing)
                        memoDao.deleteResource(existing)
                    }
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

            refreshUnsyncedCount()
            enqueuePushMemo(updatedMemo.identifier)
            ApiResponse.Success(withResources(updatedMemo))
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
            refreshUnsyncedCount()
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
            refreshUnsyncedCount()
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
            refreshUnsyncedCount()
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
            refreshUnsyncedCount()
            ApiResponse.Success(resource)
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
            refreshUnsyncedCount()
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
        return ApiResponse.Success(currentUser)
    }

    override suspend fun sync(): ApiResponse<Unit> {
        return operationMutex.withLock {
            setSyncing(true)
            try {
                val result = syncInternal()
                if (result is ApiResponse.Success) {
                    setSyncError(null)
                } else {
                    setSyncError(result.getErrorMessage())
                }
                refreshUnsyncedCount()
                result
            } catch (e: Throwable) {
                val failure = ApiResponse.Failure.Exception(e)
                setSyncError(failure.getErrorMessage())
                refreshUnsyncedCount()
                failure
            } finally {
                setSyncing(false)
            }
        }
    }

    private suspend fun syncInternal(): ApiResponse<Unit> {
        val currentUserSync = refreshCurrentUserFromRemoteStrict()
        if (currentUserSync !is ApiResponse.Success) {
            return currentUserSync
        }

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

    private suspend fun refreshCurrentUserFromRemoteStrict(): ApiResponse<Unit> {
        val remoteUser = try {
            remoteRepository.getCurrentUser()
        } catch (e: Throwable) {
            return ApiResponse.Failure.Exception(e)
        }

        return when (remoteUser) {
            is ApiResponse.Success -> {
                currentUser = remoteUser.data
                try {
                    onUserSynced(remoteUser.data)
                    ApiResponse.Success(Unit)
                } catch (e: Throwable) {
                    ApiResponse.Failure.Exception(e)
                }
            }
            is ApiResponse.Failure.Error -> {
                if (remoteUser.statusCode == StatusCode.Forbidden || remoteUser.statusCode == StatusCode.Unauthorized) {
                    ApiResponse.Failure.Exception(MoeMemosException.accessTokenInvalid)
                } else {
                    remoteUser.mapFailureToUnit()
                }
            }
            is ApiResponse.Failure.Exception -> remoteUser.mapFailureToUnit()
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
                pinned = local.pinned,
                archived = local.archived
            )
            if (updated is ApiResponse.Success) {
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
            uri = remoteResource.uri,
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
                pinned = remoteMemo.pinned,
                archived = remoteMemo.archived,
                needsSync = false,
                isDeleted = false,
                lastModified = remoteUpdatedAt,
                lastSyncedAt = remoteUpdatedAt
            )
        )

        val currentResources = memoDao.getMemoResources(localIdentifier, accountKey)
        val remoteResourceIds = remoteMemo.resources.mapTo(hashSetOf()) { remoteResourceId(it) }
        currentResources.forEach { currentResource ->
            if (currentResource.remoteId !in remoteResourceIds) {
                deleteLocalFile(currentResource)
                memoDao.deleteResource(currentResource)
            }
        }

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
                    uri = resource.uri,
                    localUri = preferredLocalUri,
                    mimeType = resource.mimeType,
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
            resource.remoteId
        }.sorted()
    }

    private suspend fun pushLocalResource(identifier: String): Boolean {
        val local = memoDao.getResourceById(identifier, accountKey) ?: return true
        val ensured = ensureUploadedResource(local, memoRemoteId = null) ?: return false
        return ensured.remoteId != null
    }

    private fun enqueuePushMemo(identifier: String, forceCreate: Boolean = false) {
        enqueueOperation("Failed to sync memo") {
            pushLocalMemo(identifier, forceCreate)
        }
    }

    private fun enqueuePushResource(identifier: String) {
        enqueueOperation("Failed to sync resource") {
            pushLocalResource(identifier)
        }
    }

    private fun enqueueDeleteRemoteResource(remoteId: String) {
        enqueueOperation("Failed to delete resource on server") {
            remoteRepository.deleteResource(remoteId) is ApiResponse.Success
        }
    }

    private fun enqueueOperation(
        defaultErrorMessage: String,
        block: suspend () -> Boolean
    ) {
        operationScope.launch {
            operationMutex.withLock {
                setSyncing(true)
                try {
                    val success = block()
                    if (success) {
                        setSyncError(null)
                    } else {
                        setSyncError(defaultErrorMessage)
                    }
                } catch (e: Throwable) {
                    setSyncError(e.localizedMessage ?: defaultErrorMessage)
                } finally {
                    refreshUnsyncedCount()
                    setSyncing(false)
                }
            }
        }
    }

    private suspend fun refreshUnsyncedCount() {
        val count = memoDao.countUnsyncedMemos(accountKey)
        _syncStatus.update { it.copy(unsyncedCount = count) }
    }

    private fun setSyncing(syncing: Boolean) {
        _syncStatus.update { it.copy(syncing = syncing) }
    }

    private fun setSyncError(message: String?) {
        _syncStatus.update { it.copy(errorMessage = message) }
    }

    private suspend fun withResources(memo: MemoEntity): MemoEntity {
        val resources = memoDao.getMemoResources(memo.identifier, accountKey)
        return memo.copy().also { it.resources = resources }
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
        return memo.remoteId.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("RemoteRepository must return memos with non-empty remoteId")
    }

    private fun remoteResourceId(resource: Resource): String {
        return resource.remoteId.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("RemoteRepository must return resources with non-empty remoteId")
    }

    private fun <T> ApiResponse<T>.mapFailureToUnit(): ApiResponse<Unit> {
        return when (this) {
            is ApiResponse.Success -> ApiResponse.Success(Unit)
            is ApiResponse.Failure.Error -> ApiResponse.Failure.Error(this.payload)
            is ApiResponse.Failure.Exception -> ApiResponse.Failure.Exception(this.throwable)
        }
    }

    override fun close() {
        operationScope.cancel()
    }

}

private fun MemoWithResources.toMemoEntity(): MemoEntity {
    return memo.copy().also { it.resources = resources }
}
