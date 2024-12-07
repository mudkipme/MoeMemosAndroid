package me.mudkip.moememos.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import me.mudkip.moememos.data.model.MemoVisibility
import java.time.Instant

@Entity(tableName = "memos")
data class MemoEntity(
    @PrimaryKey
    val identifier: String,
    val content: String,
    val date: Instant,
    val visibility: MemoVisibility,
    val creatorId: String?,
    val creatorName: String?,
    val pinned: Boolean,
    val archived: Boolean = false,
    val needsSync: Boolean = true,
    val isDeleted: Boolean = false,
    val lastModified: Instant = Instant.now()
) 