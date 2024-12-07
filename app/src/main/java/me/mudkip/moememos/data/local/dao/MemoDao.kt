package me.mudkip.moememos.data.local.dao

import me.mudkip.moememos.data.local.entity.MemoEntity
import me.mudkip.moememos.data.local.entity.ResourceEntity
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Delete

@Dao
interface MemoDao {
    @Query("SELECT * FROM memos WHERE archived = 1 ORDER BY date DESC")
    suspend fun getArchivedMemos(): List<MemoEntity>

    @Query("""
        SELECT * FROM memos 
        WHERE archived = 0 AND isDeleted = 0
        ORDER BY pinned DESC, date DESC
    """)
    suspend fun getAllMemos(): List<MemoEntity>

    @Query("SELECT * FROM memos WHERE identifier = :identifier")
    suspend fun getMemoById(identifier: String): MemoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemo(memo: MemoEntity)

    @Delete
    suspend fun deleteMemo(memo: MemoEntity)

    @Query("SELECT * FROM resources WHERE memoId = :memoId")
    suspend fun getMemoResources(memoId: String): List<ResourceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResource(resource: ResourceEntity)

    @Delete
    suspend fun deleteResource(resource: ResourceEntity)

    @Query("SELECT * FROM resources")
    suspend fun getAllResources(): List<ResourceEntity>

    @Query("SELECT * FROM resources WHERE identifier = :identifier")
    suspend fun getResourceById(identifier: String): ResourceEntity?

    @Query("SELECT * FROM memos WHERE needsSync = 1")
    suspend fun getUnsyncedMemos(): List<MemoEntity>

    @Query("UPDATE memos SET needsSync = :needsSync WHERE identifier = :identifier")
    suspend fun updateMemoSyncStatus(identifier: String, needsSync: Boolean)
}