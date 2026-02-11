package me.mudkip.moememos.data.model

import java.time.Instant

data class User(
    val identifier: String,
    val name: String,
    val startDate: Instant = Instant.now(),
    val defaultVisibility: MemoVisibility = MemoVisibility.PRIVATE,
    val avatarUrl: String? = null
)
