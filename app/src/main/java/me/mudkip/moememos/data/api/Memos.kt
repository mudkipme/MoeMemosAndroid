package me.mudkip.moememos.data.api

import androidx.annotation.Keep
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import me.mudkip.moememos.data.model.MemoVisibility

@JsonClass(generateAdapter = false)
enum class MemosRole {
    @field:Json(name = "ROLE_UNSPECIFIED")
    ROLE_UNSPECIFIED,
    @field:Json(name = "HOST")
    HOST,
    @field:Json(name = "ADMIN")
    ADMIN,
    @field:Json(name = "USER")
    USER
}

@JsonClass(generateAdapter = false)
enum class MemosVisibility {
    @field:Json(name = "VISIBILITY_UNSPECIFIED")
    VISIBILITY_UNSPECIFIED,
    @field:Json(name = "PRIVATE")
    PRIVATE,
    @field:Json(name = "PROTECTED")
    PROTECTED,
    @field:Json(name = "PUBLIC")
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

@JsonClass(generateAdapter = false)
enum class MemosRowStatus {
    @field:Json(name = "ROW_STATUS_UNSPECIFIED")
    ROW_STATUS_UNSPECIFIED,
    @field:Json(name = "NORMAL")
    NORMAL,
    @field:Json(name = "ARCHIVED")
    ARCHIVED,
    @field:Json(name = "ACTIVE")
    ACTIVE,
}

@Keep
data class MemosProfile(
    val mode: String,
    val version: String
)

@JsonClass(generateAdapter = false)
enum class MemosView {
    @field:Json(name = "MEMO_VIEW_UNSPECIFIED")
    MEMO_VIEW_UNSPECIFIED,
    @field:Json(name = "MEMO_VIEW_FULL")
    MEMO_VIEW_FULL,
    @field:Json(name = "MEMO_VIEW_METADATA_ONLY")
    MEMO_VIEW_METADATA_ONLY,
}
