package com.scentvault.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fragrances")
data class Fragrance(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val brand: String = "",
    val bottleSizeMl: Int? = null,
    val purchaseDateMillis: Long? = null,
    val batchCode: String = "",
    val notes: String = "",
    val photoPath: String? = null,
    val createdAtMillis: Long = System.currentTimeMillis()
)
