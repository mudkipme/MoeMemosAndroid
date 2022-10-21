package me.mudkip.moememos.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = false)
enum class MemosVisibility {
    @field:Json(name = "PUBLIC")
    PUBLIC,
    @field:Json(name = "PROTECTED")
    PROTECTED,
    @field:Json(name = "PRIVATE")
    PRIVATE
}

@JsonClass(generateAdapter = false)
enum class MemosRowStatus {
    @field:Json(name = "NORMAL")
    NORMAL,
    @field:Json(name = "ARCHIVED")
    ARCHIVED
}

data class Memo(
    val id: Int,
    val createdTs: Long,
    val creatorId: Int,
    var content: String,
    var pinned: Boolean,
    val rowStatus: MemosRowStatus = MemosRowStatus.NORMAL,
    val updatedTs: Long,
    val visibility: MemosVisibility = MemosVisibility.PRIVATE
)