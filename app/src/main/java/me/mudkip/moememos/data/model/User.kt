package me.mudkip.moememos.data.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable
import java.util.*

enum class MemosRole {
    @SerializedName("HOST")
    HOST,
    @SerializedName("USER")
    USER
}

enum class MemosUserSettingKey {
    @SerializedName("locale")
    LOCALE,
    @SerializedName("memoVisibility")
    MEMO_VISIBILITY,
    @SerializedName("editorFontStyle")
    EDITOR_FONT_STYLE,
    UNKNOWN
}

data class MemosUserSetting(
    val key: MemosUserSettingKey = MemosUserSettingKey.UNKNOWN,
    val value: String
): Serializable

data class User (
    val createdTs: Date,
    val email: String,
    val id: Int,
    val name: String,
    val openId: String,
    val role: MemosRole = MemosRole.USER,
    val rowStatus: MemosRowStatus = MemosRowStatus.NORMAL,
    val updatedTs: Date,
    val userSettingList: List<MemosUserSetting>? = null
): Serializable