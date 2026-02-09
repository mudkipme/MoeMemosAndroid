package me.mudkip.moememos.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import me.mudkip.moememos.data.local.entity.MemoEntity
import me.mudkip.moememos.data.local.entity.MemoWithResources
import me.mudkip.moememos.data.local.entity.ResourceEntity
import kotlinx.coroutines.flow.Flow

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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemo(memo: MemoEntity)

    @Delete
    suspend fun deleteMemo(memo: MemoEntity)

    @Query("SELECT * FROM resources WHERE memoId = :memoId AND accountKey = :accountKey")
    suspend fun getMemoResources(memoId: String, accountKey: String): List<ResourceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResource(resource: ResourceEntity)

    @Delete
    suspend fun deleteResource(resource: ResourceEntity)

    @Query("SELECT * FROM resources WHERE accountKey = :accountKey")
    suspend fun getAllResources(accountKey: String): List<ResourceEntity>

    @Query("SELECT * FROM resources WHERE identifier = :identifier AND accountKey = :accountKey")
    suspend fun getResourceById(identifier: String, accountKey: String): ResourceEntity?

    @Query("SELECT * FROM resources WHERE remoteId = :remoteId AND accountKey = :accountKey")
    suspend fun getResourceByRemoteId(remoteId: String, accountKey: String): ResourceEntity?

}
