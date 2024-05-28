package me.mudkip.moememos.data.model

import java.util.Date

enum class MemoVisibility {
    PRIVATE,
    PROTECTED,
    PUBLIC
}

data class Memo(
    val identifier: String,
    val content: String,
    val date: Date,
    val pinned: Boolean,
    val visibility: MemoVisibility,
    val resources: List<Resource>,
    val tags: List<String>
)