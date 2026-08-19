package com.scentvault.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {

    @Query("SELECT * FROM tags ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<Tag>>

    @Query("SELECT * FROM tags WHERE name = :name LIMIT 1")
    suspend fun findByName(name: String): Tag?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(tag: Tag): Long

    @Query("DELETE FROM tags")
    suspend fun deleteAll()

    @Query("""
        DELETE FROM tags WHERE id NOT IN (SELECT DISTINCT tagId FROM fragrance_tag_cross_ref)
    """)
    suspend fun deleteOrphans()
}
