package com.scentvault.app.data

import androidx.room.Entity

@Entity(tableName = "fragrance_tag_cross_ref", primaryKeys = ["fragranceId", "tagId"])
data class FragranceTagCrossRef(
    val fragranceId: Long,
    val tagId: Long
)
