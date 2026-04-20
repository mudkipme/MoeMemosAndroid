package me.mudkip.moememos.data.model

import kotlinx.serialization.Serializable
import java.net.URI

@Serializable
data class MemosAccount(
    val host: String = "",
    val accessToken: String = "",
    val id: Long = 0L,
    val name: String = "",
    val avatarUrl: String = "",
    val startDateEpochSecond: Long = 0L,
    val defaultVisibility: String = MemoVisibility.PRIVATE.name,
    val accountLabel: String = "",
)

fun MemosAccount.displayTitle(): String {
    val label = accountLabel.trim()
    if (label.isNotEmpty()) return label
    val n = name.trim()
    if (n.isNotEmpty()) return n
    val h = host.trim()
    if (h.isEmpty()) return ""
    return try {
        URI(h).host ?: h
    } catch (_: Exception) {
        h
    }
}
