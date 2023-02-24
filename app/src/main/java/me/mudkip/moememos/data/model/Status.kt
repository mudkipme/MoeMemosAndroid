package me.mudkip.moememos.data.model

import androidx.annotation.Keep

@Keep
data class Profile(
    val mode: String,
    val version: String
)

@Keep
data class Status(
    val host: User,
    val profile: Profile
)