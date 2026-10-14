package me.mudkip.moememos.util

import me.mudkip.moememos.data.local.entity.MemoEntity
import me.mudkip.moememos.data.model.MemoVisibility
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class MemoSearchTest {
    private fun memo(id: String, content: String, archived: Boolean = true, deleted: Boolean = false) =
        MemoEntity(identifier = id, accountKey = "a", content = content, date = Instant.EPOCH,
            visibility = MemoVisibility.PRIVATE, pinned = false, archived = archived, isDeleted = deleted)

    private val list = listOf(
        memo("1", "Buy Yale battery"), memo("2", "other note"),
        memo("3", "battery deleted", deleted = true), memo("4", "battery active", archived = false),
    )

    @Test fun disabled_returnsEmpty() = assertTrue(archivedSearchResults(list, "battery", false).isEmpty())
    @Test fun emptyQuery_returnsEmpty() {
        assertTrue(archivedSearchResults(list, "", true).isEmpty())
        assertTrue(archivedSearchResults(list, null, true).isEmpty())
    }
    @Test fun enabled_matchesCaseInsensitive_excludesDeletedAndActive() =
        assertEquals(listOf("1"), archivedSearchResults(list, "BATTERY", true).map { it.identifier })
}
