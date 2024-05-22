package me.mudkip.moememos.data.api

import androidx.annotation.Keep
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = false)
enum class MemosRole {
    @field:Json(name = "HOST")
    HOST,
    @field:Json(name = "USER")
    USER
}

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

@Keep
data class MemosProfile(
    val mode: String,
    val version: String
)