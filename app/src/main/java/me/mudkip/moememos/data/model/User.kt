package me.mudkip.moememos.data.model

import java.util.Date

data class User(
    val identifier: String,
    val name: String,
    val startDate: Date = Date(),
    val defaultVisibility: MemoVisibility = MemoVisibility.PRIVATE
)
