package me.mudkip.moememos.data.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable
import java.util.*

enum class MemosVisibility {
    @SerializedName("PUBLIC")
    PUBLIC,
    @SerializedName("PROTECTED")
    PROTECTED,
    @SerializedName("PRIVATE")
    PRIVATE
}

enum class MemosRowStatus {
    @SerializedName("NORMAL")
    NORMAL,
    @SerializedName("ARCHIVED")
    ARCHIVED
}

data class Memo(
    val id: Int,
    val createdTs: Date,
    val creatorId: Int,
    var content: String,
    var pinned: Boolean,
    val rowStatus: MemosRowStatus = MemosRowStatus.NORMAL,
    val updatedTs: Date,
    val visibility: MemosVisibility = MemosVisibility.PRIVATE
): Serializable