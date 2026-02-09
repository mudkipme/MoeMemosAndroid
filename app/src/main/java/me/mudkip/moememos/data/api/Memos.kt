package me.mudkip.moememos.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.mudkip.moememos.data.model.MemoVisibility

@Serializable
enum class MemosRole {
    @SerialName("ROLE_UNSPECIFIED")
    ROLE_UNSPECIFIED,
    @SerialName("HOST")
    HOST,
    @SerialName("ADMIN")
    ADMIN,
    @SerialName("USER")
    USER
}

@Serializable
enum class MemosVisibility {
    @SerialName("VISIBILITY_UNSPECIFIED")
    VISIBILITY_UNSPECIFIED,
    @SerialName("PRIVATE")
    PRIVATE,
    @SerialName("PROTECTED")
    PROTECTED,
    @SerialName("PUBLIC")
    PUBLIC;

    fun toMemoVisibility(): MemoVisibility = when (this) {
        PRIVATE -> MemoVisibility.PRIVATE
        PROTECTED -> MemoVisibility.PROTECTED
        PUBLIC -> MemoVisibility.PUBLIC
        else -> MemoVisibility.PRIVATE
    }

    companion object {
        fun fromMemoVisibility(visibility: MemoVisibility): MemosVisibility = when (visibility) {
            MemoVisibility.PRIVATE -> PRIVATE
            MemoVisibility.PROTECTED -> PROTECTED
            MemoVisibility.PUBLIC -> PUBLIC
        }
    }
}

@Serializable
enum class MemosRowStatus {
    @SerialName("ROW_STATUS_UNSPECIFIED")
    ROW_STATUS_UNSPECIFIED,
    @SerialName("NORMAL")
    NORMAL,
    @SerialName("ARCHIVED")
    ARCHIVED,
    @SerialName("ACTIVE")
    ACTIVE,
}

@Serializable
data class MemosProfile(
    val version: String
)

@Serializable
enum class MemosView {
    @SerialName("MEMO_VIEW_UNSPECIFIED")
    MEMO_VIEW_UNSPECIFIED,
    @SerialName("MEMO_VIEW_FULL")
    MEMO_VIEW_FULL,
    @SerialName("MEMO_VIEW_METADATA_ONLY")
    MEMO_VIEW_METADATA_ONLY,
}
