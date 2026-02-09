package me.mudkip.moememos.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

data class MemoWithResources(
    @Embedded val memo: MemoEntity,
    @Relation(
        parentColumn = "identifier",
        entityColumn = "memoId"
    )
    val resources: List<ResourceEntity>
)
