package me.mudkip.moememos.data.model

import java.time.Instant

enum class MemoVisibility {
    PRIVATE,
    PROTECTED,
    PUBLIC
}

data class Memo(
    val identifier: String,
    val remoteId: String? = null,
    val content: String,
    val date: Instant,
    val pinned: Boolean,
    val visibility: MemoVisibility,
    val resources: List<Resource>,
    val tags: List<String>,
    val creator: User? = null,
    val archived: Boolean = false,
    val updatedAt: Instant? = null
)
