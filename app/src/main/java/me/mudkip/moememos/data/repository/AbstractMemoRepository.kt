package me.mudkip.moememos.data.repository

import me.mudkip.moememos.data.model.Memo
import me.mudkip.moememos.data.model.MemoVisibility
import me.mudkip.moememos.data.model.Resource
import me.mudkip.moememos.data.model.User

abstract class AbstractMemoRepository {
    abstract suspend fun listMemos(): List<Memo>
    abstract suspend fun listArchivedMemos(): List<Memo>
    abstract suspend fun createMemo(content: String, visibility: MemoVisibility, resources: List<Resource>): Memo
    abstract suspend fun updateMemo(identifier: String, content: String? = null, resources: List<Resource>? = null, visibility: MemoVisibility?, tags: List<String>?, pinned: Boolean?): Memo
    abstract suspend fun deleteMemo(identifier: String)
    abstract suspend fun archiveMemo(identifier: String)
    abstract suspend fun restoreMemo(identifier: String)

    abstract suspend fun listTags(): List<String>
    abstract suspend fun deleteTag(name: String)

    abstract suspend fun listResources(): List<Resource>
    abstract suspend fun createResource(filename: String, type: String, content: ByteArray, memoIdentifier: String?): Resource
    abstract suspend fun deleteResource(identifier: String)

    abstract suspend fun getCurrentUser(): List<User>
}