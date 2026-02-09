package me.mudkip.moememos.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(
    tableName = "resources",
    indices = [
        Index(value = ["memoId"]),
        Index(value = ["accountKey"]),
        Index(value = ["accountKey", "remoteId"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = MemoEntity::class,
            parentColumns = ["identifier"],
            childColumns = ["memoId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ResourceEntity(
    @PrimaryKey
    val identifier: String,
    val remoteId: String? = null,
    val accountKey: String,
    val date: Instant,
    val filename: String,
    val uri: String,
    val localUri: String? = null,
    val mimeType: String?,
    val memoId: String
) 
