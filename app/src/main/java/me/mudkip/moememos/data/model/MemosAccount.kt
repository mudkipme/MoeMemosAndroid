package me.mudkip.moememos.data.model

import kotlinx.serialization.Serializable

@Serializable
data class MemosAccount(
    val host: String = "",
    val accessToken: String = "",
    val id: Long = 0L,
    val name: String = "",
    val avatarUrl: String = "",
)
