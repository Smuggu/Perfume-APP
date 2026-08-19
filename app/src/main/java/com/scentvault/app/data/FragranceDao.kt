package com.scentvault.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FragranceDao {

    @Transaction
    @Query("SELECT * FROM fragrances ORDER BY name COLLATE NOCASE ASC")
    fun observeAllWithTags(): Flow<List<FragranceWithTags>>

    @Transaction
    @Query("SELECT * FROM fragrances WHERE id = :id")
    fun observeOneWithTags(id: Long): Flow<FragranceWithTags?>

    @Transaction
    @Query("SELECT * FROM fragrances")
    suspend fun getAllWithTagsOnce(): List<FragranceWithTags>

    @Insert
    suspend fun insert(fragrance: Fragrance): Long

    @Update
    suspend fun update(fragrance: Fragrance)

    @Delete
    suspend fun delete(fragrance: Fragrance)

    @Query("DELETE FROM fragrance_tag_cross_ref WHERE fragranceId = :fragranceId")
    suspend fun clearTagsFor(fragranceId: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCrossRef(crossRef: FragranceTagCrossRef)

    @Query("DELETE FROM fragrances")
    suspend fun deleteAll()

    @Query("DELETE FROM fragrance_tag_cross_ref")
    suspend fun deleteAllCrossRefs()
}
