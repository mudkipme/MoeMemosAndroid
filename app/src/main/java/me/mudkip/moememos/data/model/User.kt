package me.mudkip.moememos.data.model

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
enum class MemosUserSettingKey {
    @field:Json(name = "locale")
    LOCALE,
    @field:Json(name = "memoVisibility")
    MEMO_VISIBILITY,
    @field:Json(name = "editorFontStyle")
    EDITOR_FONT_STYLE,
    UNKNOWN
}

data class MemosUserSetting(
    val key: MemosUserSettingKey = MemosUserSettingKey.UNKNOWN,
    val value: String
)

data class User(
    val createdTs: Long,
    val email: String,
    val id: Int,
    val name: String,
    val openId: String,
    val role: MemosRole = MemosRole.USER,
    val rowStatus: MemosRowStatus = MemosRowStatus.NORMAL,
    val updatedTs: Long,
    val userSettingList: List<MemosUserSetting>? = null
)