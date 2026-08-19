package com.scentvault.app.data

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class FragranceWithTags(
    @Embedded
    val fragrance: Fragrance,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = FragranceTagCrossRef::class,
            parentColumn = "fragranceId",
            entityColumn = "tagId"
        )
    )
    val tags: List<Tag>
)
