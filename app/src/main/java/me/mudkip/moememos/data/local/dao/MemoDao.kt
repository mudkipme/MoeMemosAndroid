package me.mudkip.moememos.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import me.mudkip.moememos.data.local.entity.MemoEntity
import me.mudkip.moememos.data.local.entity.MemoWithResources
import me.mudkip.moememos.data.local.entity.ResourceEntity
import java.time.Instant

@Dao
interface MemoDao {
    @Query("SELECT * FROM memos WHERE accountKey = :accountKey AND archived = 1 ORDER BY date DESC")
    suspend fun getArchivedMemos(accountKey: String): List<MemoEntity>

    @Query("""
        SELECT * FROM memos 
        WHERE accountKey = :accountKey AND archived = 0 AND isDeleted = 0
        ORDER BY pinned DESC, date DESC
    """)
    suspend fun getAllMemos(accountKey: String): List<MemoEntity>

    @Transaction
    @Query("""
        SELECT * FROM memos
        WHERE accountKey = :accountKey AND archived = 0 AND isDeleted = 0
        ORDER BY pinned DESC, date DESC
    """)
    fun observeAllMemos(accountKey: String): Flow<List<MemoWithResources>>

    @Query("SELECT * FROM memos WHERE accountKey = :accountKey")
    suspend fun getAllMemosForSync(accountKey: String): List<MemoEntity>

    @Query("SELECT COUNT(*) FROM memos WHERE accountKey = :accountKey AND needsSync = 1")
    suspend fun countUnsyncedMemos(accountKey: String): Int

    @Query("SELECT * FROM memos WHERE identifier = :identifier AND accountKey = :accountKey")
    suspend fun getMemoById(identifier: String, accountKey: String): MemoEntity?

    @Query("SELECT * FROM memos WHERE remoteId = :remoteId AND accountKey = :accountKey")
    suspend fun getMemoByRemoteId(remoteId: String, accountKey: String): MemoEntity?

    @Upsert
    suspend fun insertMemo(memo: MemoEntity)

    // Imports must never replace a row, including an identifier owned by another account.
    @Insert
    suspend fun insertImportedMemo(memo: MemoEntity)

    @Insert
    suspend fun insertImportedResource(resource: ResourceEntity)

    /**
     * Upserts [memo] only if its stored row still has [expectedLastModified], i.e. nothing wrote the
     * row since the caller read it. Returns false (and writes nothing) otherwise or if the row is gone.
     */
    @Transaction
    suspend fun insertMemoIfUnchanged(memo: MemoEntity, expectedLastModified: Instant): Boolean {
        val current = getMemoById(memo.identifier, memo.accountKey) ?: return false
        if (current.lastModified != expectedLastModified) {
            return false
        }
        insertMemo(memo)
        return true
    }

    @Delete
    suspend fun deleteMemo(memo: MemoEntity)

    @Query("SELECT * FROM resources WHERE memoId = :memoId AND accountKey = :accountKey")
    suspend fun getMemoResources(memoId: String, accountKey: String): List<ResourceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResource(resource: ResourceEntity)

    @Transaction
    suspend fun attachResourceToMemo(resource: ResourceEntity, memoId: String, accountKey: String) {
        // The editor may still hold the pre-upload snapshot. Keep the stored server metadata.
        val current = getResourceById(resource.identifier, accountKey) ?: resource
        insertResource(current.copy(accountKey = accountKey, memoId = memoId))
    }

    @Transaction
    suspend fun recordResourceUpload(
        identifier: String,
        accountKey: String,
        remoteId: String,
        uri: String
    ): ResourceEntity? {
        // The resource may have been attached or removed while the upload was in flight.
        val current = getResourceById(identifier, accountKey) ?: return null
        val uploaded = current.copy(remoteId = remoteId, uri = uri, localUri = current.localUri ?: current.uri)
        insertResource(uploaded)
        return uploaded
    }

    @Delete
    suspend fun deleteResource(resource: ResourceEntity)

    @Query("SELECT * FROM resources WHERE accountKey = :accountKey ORDER BY date DESC")
    suspend fun getAllResources(accountKey: String): List<ResourceEntity>

    @Query("SELECT * FROM resources WHERE identifier = :identifier AND accountKey = :accountKey")
    suspend fun getResourceById(identifier: String, accountKey: String): ResourceEntity?

    @Query("SELECT * FROM resources WHERE remoteId = :remoteId AND accountKey = :accountKey")
    suspend fun getResourceByRemoteId(remoteId: String, accountKey: String): ResourceEntity?

    @Query("DELETE FROM resources WHERE accountKey = :accountKey")
    suspend fun deleteResourcesByAccount(accountKey: String)

    @Query("DELETE FROM memos WHERE accountKey = :accountKey")
    suspend fun deleteMemosByAccount(accountKey: String)

}
