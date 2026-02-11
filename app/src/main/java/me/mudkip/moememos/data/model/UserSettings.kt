package me.mudkip.moememos.data.model

import kotlinx.serialization.Serializable

@Serializable
data class UserSettings(
    val draft: String = "",
    val acceptedUnsupportedSyncVersions: List<String> = emptyList(),
)
