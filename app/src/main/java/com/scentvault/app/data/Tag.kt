package com.scentvault.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tags", indices = [androidx.room.Index(value = ["name"], unique = true)])
data class Tag(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String
)
