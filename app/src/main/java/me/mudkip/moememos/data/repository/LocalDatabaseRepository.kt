package me.mudkip.moememos.data.repository

import android.net.Uri
import com.skydoves.sandwich.ApiResponse
import me.mudkip.moememos.data.local.dao.MemoDao
import me.mudkip.moememos.data.local.entity.MemoEntity
import me.mudkip.moememos.data.local.entity.ResourceEntity
import me.mudkip.moememos.data.local.FileStorage
import me.mudkip.moememos.data.model.Memo
import me.mudkip.moememos.data.model.MemoVisibility
import me.mudkip.moememos.data.model.Resource
import me.mudkip.moememos.data.model.User
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import java.time.Instant
import java.util.UUID
import me.mudkip.moememos.data.local.UserPreferences
import me.mudkip.moememos.data.model.Account

class LocalDatabaseRepository(
    private val memoDao: MemoDao,
    private val fileStorage: FileStorage,
    private val userPreferences: UserPreferences,
    private val account: Account? = null
) : AbstractMemoRepository() {
    
    override suspend fun listMemos(): ApiResponse<List<Memo>> {
        return try {
            val memos = memoDao.getAllMemos()
                .filter { !it.isDeleted }  // Filter out deleted memos
                .map { entity ->
                    convertToMemo(entity)
                }
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
            // Get user locally without network call
            val user = when (val userResponse = getCurrentUser()) {
                is ApiResponse.Success -> userResponse.data
                else -> User("local", "Local User") // Fallback to local user if getCurrentUser fails
            }
            
            val now = Instant.now()
            val memo = MemoEntity(
                identifier = UUID.randomUUID().toString(),
                content = content,
                date = now,
                visibility = visibility,
                creatorId = user.identifier,
                creatorName = user.name,
                pinned = false,
                archived = false,
                needsSync = true,  // Explicitly set needsSync flag
                isDeleted = false,
                lastModified = now
            )
            
            // Save memo locally
            memoDao.insertMemo(memo)
            
            // Handle resources locally
            resources.forEach { resource ->
                if (resource.uri.scheme == "content" || resource.uri.scheme == "file") {
                    // Resource is already local, just link it
                    val resourceEntity = ResourceEntity(
                        identifier = resource.identifier,
                        date = resource.date,
                        filename = resource.filename,
                        mimeType = resource.mimeType?.toString(),
                        uri = resource.uri.toString(),
                        memoId = memo.identifier
                    )
                    memoDao.insertResource(resourceEntity)
                }
            }
            
            ApiResponse.Success(convertToMemo(memo))
        } catch (e: Exception) {
            ApiResponse.Failure.Exception(e)
        }
    }

    override suspend fun listArchivedMemos(): ApiResponse<List<Memo>> {
        return try {
            val memos = memoDao.getArchivedMemos().map { entity ->
                convertToMemo(entity)
            }
            ApiResponse.Success(memos)
        } catch (e: Exception) {
            ApiResponse.Failure.Exception(e)
        }
    }

    override suspend fun listWorkspaceMemos(
        pageSize: Int,
        pageToken: String?
    ): ApiResponse<Pair<List<Memo>, String?>> {
        return try {
            val memos = memoDao.getAllMemos().map { entity ->
                Memo(
                    identifier = entity.identifier,
                    content = entity.content,
                    date = entity.date,
                    pinned = entity.pinned,
                    visibility = entity.visibility,
                    resources = memoDao.getMemoResources(entity.identifier).map { convertToResource(it) },
                    tags = emptyList(), // Local storage doesn't support tags yet
                    creator = if (entity.creatorId != null && entity.creatorName != null) {
                        User(entity.creatorId, entity.creatorName)
                    } else null
                )
            }
            ApiResponse.Success(memos to null)
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
            val existingMemo = memoDao.getMemoById(identifier) ?: return ApiResponse.Failure.Exception(Exception("Memo not found"))
            
            val updatedMemo = existingMemo.copy(
                content = content ?: existingMemo.content,
                visibility = visibility ?: existingMemo.visibility,
                pinned = pinned ?: existingMemo.pinned,
                needsSync = true,  // Mark as needing sync
                lastModified = Instant.now()
            )
            
            memoDao.insertMemo(updatedMemo)
            
            if (resources != null) {
                // Delete existing resources
                memoDao.getMemoResources(identifier).forEach {
                    memoDao.deleteResource(it)
                }
                
                // Insert new resources
                resources.forEach { resource ->
                    memoDao.insertResource(convertToResourceEntity(resource, identifier))
                }
            }
            
            ApiResponse.Success(convertToMemo(updatedMemo))
        } catch (e: Exception) {
            ApiResponse.Failure.Exception(e)
        }
    }

    override suspend fun listTags(): ApiResponse<List<String>> {
        return ApiResponse.Success(emptyList()) // Local storage doesn't support tags yet
    }

    override suspend fun deleteTag(name: String): ApiResponse<Unit> {
        return ApiResponse.Success(Unit) // Local storage doesn't support tags yet
    }

    override suspend fun listResources(): ApiResponse<List<Resource>> {
        return try {
            val resources = memoDao.getAllResources()
            ApiResponse.Success(resources.map { convertToResource(it) })
        } catch (e: Exception) {
            ApiResponse.Failure.Exception(e)
        }
    }

    suspend fun logout(): ApiResponse<Unit> {
        return try {
            userPreferences.saveUser("local", "Local User")
            ApiResponse.Success(Unit)
        } catch (e: Exception) {
            ApiResponse.Failure.Exception(e)
        }
    }

    override suspend fun deleteMemo(identifier: String): ApiResponse<Unit> {
        return try {
            val memo = memoDao.getMemoById(identifier) ?: return ApiResponse.Failure.Exception(Exception("Memo not found"))
            // Mark as deleted and unsynced instead of immediate deletion
            val deletedMemo = memo.copy(isDeleted = true, needsSync = true)
            memoDao.insertMemo(deletedMemo)
            ApiResponse.Success(Unit)
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
            val uri = fileStorage.saveFile(content, UUID.randomUUID().toString() + "_" + filename)
            val resource = ResourceEntity(
                identifier = UUID.randomUUID().toString(),
                date = Instant.now(),
                filename = filename,
                uri = uri.toString(),
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
            val resource = memoDao.getResourceById(identifier) 
                ?: return ApiResponse.Failure.Exception(Exception("Resource not found"))
            fileStorage.deleteFile(Uri.parse(resource.uri))
            memoDao.deleteResource(resource)
            ApiResponse.Success(Unit)
        } catch (e: Exception) {
            ApiResponse.Failure.Exception(e)
        }
    }

    override suspend fun getCurrentUser(): ApiResponse<User> {
        return try {
            // First try to get user from account
            when (account) {
                is Account.MemosV0 -> {
                    ApiResponse.Success(User(
                        identifier = account.info.id.toString(),
                        name = account.info.name
                    ))
                }
                is Account.MemosV1 -> {
                    ApiResponse.Success(User(
                        identifier = account.info.id.toString(),
                        name = account.info.name
                    ))
                }
                else -> {
                    // Fallback to stored preferences
                    val user = userPreferences.getUser()
                    if (user != null) {
                        ApiResponse.Success(User(user.first, user.second))
                    } else {
                        ApiResponse.Failure.Exception(Exception("No user found"))
                    }
                }
            }
        } catch (e: Exception) {
            ApiResponse.Failure.Exception(e)
        }
    }

    override suspend fun archiveMemo(identifier: String): ApiResponse<Unit> {
        return try {
            val memo = memoDao.getMemoById(identifier) 
                ?: return ApiResponse.Failure.Exception(Exception("Memo not found"))
            val archivedMemo = memo.copy(
                archived = true,
                needsSync = true,
                lastModified = Instant.now()
            )
            memoDao.insertMemo(archivedMemo)
            ApiResponse.Success(Unit)
        } catch (e: Exception) {
            ApiResponse.Failure.Exception(e)
        }
    }

    override suspend fun restoreMemo(identifier: String): ApiResponse<Unit> {
        return try {
            val memo = memoDao.getMemoById(identifier) 
                ?: return ApiResponse.Failure.Exception(Exception("Memo not found"))
            val restoredMemo = memo.copy(
                archived = false,
                needsSync = true,
                lastModified = Instant.now()
            )
            memoDao.insertMemo(restoredMemo)
            ApiResponse.Success(Unit)
        } catch (e: Exception) {
            ApiResponse.Failure.Exception(e)
        }
    }

    suspend fun getUnsyncedMemos(): List<MemoEntity> {
        return memoDao.getUnsyncedMemos()
    }

    suspend fun storeSyncedMemos(memos: List<Memo>) {
        val existingMemos = memoDao.getAllMemos().associateBy { it.identifier }
        
        memos.forEach { memo ->
            val existingMemo = existingMemos[memo.identifier]
            
            // Skip if memo is marked for deletion locally
            if (existingMemo?.isDeleted == true) {
                return@forEach
            }
            
            // Skip if memo exists locally and needs sync (local changes pending)
            if (existingMemo?.needsSync == true) {
                return@forEach
            }
            
            // Create or update memo
            val memoEntity = MemoEntity(
                identifier = memo.identifier,
                content = memo.content,
                date = memo.date,
                visibility = memo.visibility,
                creatorId = memo.creator?.identifier,
                creatorName = memo.creator?.name,
                pinned = memo.pinned,
                archived = existingMemo?.archived ?: false,
                needsSync = false,
                isDeleted = false,
                lastModified = Instant.now()
            )
            memoDao.insertMemo(memoEntity)
            
            // Handle resources
            memoDao.getMemoResources(memo.identifier).forEach {
                memoDao.deleteResource(it)
            }
            memo.resources.forEach { resource ->
                val resourceEntity = ResourceEntity(
                    identifier = resource.identifier,
                    date = resource.date,
                    filename = resource.filename,
                    mimeType = resource.mimeType?.toString(),
                    uri = resource.uri.toString(),
                    memoId = memo.identifier
                )
                memoDao.insertResource(resourceEntity)
            }
        }
    }

    private suspend fun convertToMemo(entity: MemoEntity): Memo {
        val resources = memoDao.getMemoResources(entity.identifier).map { convertToResource(it) }
        return Memo(
            identifier = entity.identifier,
            content = entity.content,
            date = entity.date,
            pinned = entity.pinned,
            visibility = entity.visibility,
            resources = resources,
            tags = emptyList(),
            creator = if (entity.creatorId != null && entity.creatorName != null) {
                User(entity.creatorId, entity.creatorName)
            } else null
        )
    }

    private fun convertToResource(entity: ResourceEntity): Resource {
        return Resource(
            identifier = entity.identifier,
            date = entity.date,
            filename = entity.filename,
            mimeType = entity.mimeType?.toMediaTypeOrNull(),
            uri = Uri.parse(entity.uri)
        )
    }

    private fun convertToResourceEntity(resource: Resource, memoId: String): ResourceEntity {
        return ResourceEntity(
            identifier = resource.identifier,
            date = resource.date,
            filename = resource.filename,
            mimeType = resource.mimeType?.toString(),
            uri = resource.uri.toString(),
            memoId = memoId
        )
    }

    suspend fun permanentlyDeleteMemo(identifier: String) {
        val memo = memoDao.getMemoById(identifier) ?: return
        memoDao.deleteMemo(memo)
    }

}