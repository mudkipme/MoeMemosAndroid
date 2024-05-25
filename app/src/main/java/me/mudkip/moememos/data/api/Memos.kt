package me.mudkip.moememos.data.api

import androidx.annotation.Keep
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

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
    PUBLIC
}

@JsonClass(generateAdapter = false)
enum class MemosRowStatus {
    @field:Json(name = "ROW_STATUS_UNSPECIFIED")
    ROW_STATUS_UNSPECIFIED,
    @field:Json(name = "NORMAL")
    NORMAL,
    @field:Json(name = "ARCHIVED")
    ARCHIVED
}

@Keep
data class MemosProfile(
    val mode: String,
    val version: String
)