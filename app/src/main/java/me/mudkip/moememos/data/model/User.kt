package me.mudkip.moememos.data.model

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
enum class MemosUserSettingKey {
    @field:Json(name = "locale")
    LOCALE,
    @field:Json(name = "memo-visibility")
    MEMO_VISIBILITY,
    @field:Json(name = "editorFontStyle")
    EDITOR_FONT_STYLE,
    UNKNOWN
}

@Keep
data class MemosUserSetting(
    val key: MemosUserSettingKey = MemosUserSettingKey.UNKNOWN,
    val value: String
)

@Keep
data class User(
    val createdTs: Long,
    val email: String?,
    val id: Long,
    val name: String?,
    val role: MemosRole = MemosRole.USER,
    val rowStatus: MemosRowStatus = MemosRowStatus.NORMAL,
    val updatedTs: Long?,
    val userSettingList: List<MemosUserSetting>? = null,
    val nickname: String?,
    val username: String?,
) {
    val displayEmail get() = email ?: username ?: ""
    val displayName get() = nickname ?: name ?: ""

    val memoVisibility: MemosVisibility get() = userSettingList?.find { it.key == MemosUserSettingKey.MEMO_VISIBILITY }?.let {
        try {
            MemosVisibility.valueOf(it.value.removePrefix("\"").removeSuffix("\""))
        } catch (_: IllegalArgumentException) {
            MemosVisibility.PRIVATE
        }
    } ?: MemosVisibility.PRIVATE
}