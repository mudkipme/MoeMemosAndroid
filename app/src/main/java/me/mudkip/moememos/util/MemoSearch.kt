package me.mudkip.moememos.util

import me.mudkip.moememos.data.local.entity.MemoEntity

fun memoMatchesSearch(content: String, query: String?): Boolean =
    query.isNullOrEmpty() || content.contains(query, ignoreCase = true)

/** Archived memos to append to search results; empty unless enabled AND a query is typed. */
fun archivedSearchResults(
    archived: List<MemoEntity>,
    query: String?,
    enabled: Boolean,
): List<MemoEntity> {
    if (!enabled || query.isNullOrEmpty()) return emptyList()
    return archived.filter { it.archived && !it.isDeleted && memoMatchesSearch(it.content, query) }
}
