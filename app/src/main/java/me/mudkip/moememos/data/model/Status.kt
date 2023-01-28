package me.mudkip.moememos.data.model

import androidx.annotation.Keep

@Keep
data class Profile(
    val data: String,
    val dsn: String,
    val mode: String,
    val port: Int,
    val version: String
)

@Keep
data class Status(
    val host: User,
    val profile: Profile
)