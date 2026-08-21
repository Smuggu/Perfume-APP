package com.scentvault.app.data

import kotlinx.coroutines.flow.Flow

class FragranceRepository(
    private val fragranceDao: FragranceDao,
    private val tagDao: TagDao
) {
    fun observeAllWithTags(): Flow<List<FragranceWithTags>> = fragranceDao.observeAllWithTags()

    fun observeOneWithTags(id: Long): Flow<FragranceWithTags?> = fragranceDao.observeOneWithTags(id)

    fun observeAllTags(): Flow<List<Tag>> = tagDao.observeAll()

    suspend fun observeAllWithTagsOnce(): List<FragranceWithTags> = fragranceDao.getAllWithTagsOnce()

    /** Inserts or updates [fragrance] and replaces its tag associations with [tagNames]. */
    suspend fun save(fragrance: Fragrance, tagNames: List<String>): Long {
        val id = if (fragrance.id == 0L) {
            fragranceDao.insert(fragrance)
        } else {
            fragranceDao.update(fragrance)
            fragrance.id
        }

        fragranceDao.clearTagsFor(id)
        for (rawName in tagNames) {
            val name = rawName.trim()
            if (name.isEmpty()) continue
            val existing = tagDao.findByName(name)
            val tagId = existing?.id ?: tagDao.insert(Tag(name = name))
            fragranceDao.insertCrossRef(FragranceTagCrossRef(fragranceId = id, tagId = tagId))
        }
        tagDao.deleteOrphans()
        return id
    }

    suspend fun delete(fragrance: Fragrance) {
        fragranceDao.delete(fragrance)
        tagDao.deleteOrphans()
    }

    suspend fun replaceAll(entries: List<Pair<Fragrance, List<String>>>) {
        fragranceDao.deleteAllCrossRefs()
        fragranceDao.deleteAll()
        tagDao.deleteAll()
        for ((fragrance, tagNames) in entries) {
            save(fragrance.copy(id = 0L), tagNames)
        }
    }
}
