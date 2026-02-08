package me.mudkip.moememos.data.model

import kotlinx.serialization.Serializable

@Serializable
data class LocalAccount(
    val enabled: Boolean = true,
)
