package com.scentvault.app.backup

import android.content.Context
import android.net.Uri
import com.scentvault.app.data.Fragrance
import com.scentvault.app.data.FragranceRepository
import com.scentvault.app.photo.PhotoStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Exports/imports the entire collection (database rows + bottle photos) as a single .zip
 * so the user can move their data via any file provider (Drive, local storage, email, etc.).
 */
object BackupManager {
    private const val DATA_ENTRY = "data.json"
    private const val PHOTOS_DIR = "photos/"
    private const val FORMAT_VERSION = 1

    data class Result(val fragranceCount: Int)

    suspend fun export(context: Context, repository: FragranceRepository, destination: Uri): Result =
        withContext(Dispatchers.IO) {
            val allWithTags = repository.observeAllWithTagsOnce()
            val fragrances = allWithTags.map { it.fragrance }
            val tagsByFragranceId = allWithTags.associate { it.fragrance.id to it.tags.map { tag -> tag.name } }

            val root = JSONObject()
            root.put("version", FORMAT_VERSION)
            root.put("exportedAtMillis", System.currentTimeMillis())

            val array = JSONArray()
            for (f in fragrances) {
                val obj = JSONObject()
                obj.put("name", f.name)
                obj.put("brand", f.brand)
                obj.put("bottleSizeMl", f.bottleSizeMl ?: JSONObject.NULL)
                obj.put("purchaseDateMillis", f.purchaseDateMillis ?: JSONObject.NULL)
                obj.put("batchCode", f.batchCode)
                obj.put("notes", f.notes)
                obj.put("photoFile", f.photoPath ?: JSONObject.NULL)
                obj.put("tags", JSONArray(tagsByFragranceId[f.id].orEmpty()))
                array.put(obj)
            }
            root.put("fragrances", array)

            context.contentResolver.openOutputStream(destination)?.use { rawOut ->
                ZipOutputStream(rawOut).use { zip ->
                    zip.putNextEntry(ZipEntry(DATA_ENTRY))
                    zip.write(root.toString().toByteArray(Charsets.UTF_8))
                    zip.closeEntry()

                    val photoNames = fragrances.mapNotNull { it.photoPath }.toSet()
                    for (name in photoNames) {
                        val file = PhotoStore.fileFor(context, name)
                        if (!file.exists()) continue
                        zip.putNextEntry(ZipEntry(PHOTOS_DIR + name))
                        file.inputStream().use { it.copyTo(zip) }
                        zip.closeEntry()
                    }
                }
            } ?: throw java.io.IOException("Could not open destination for writing")

            Result(fragrances.size)
        }

    suspend fun import(context: Context, repository: FragranceRepository, source: Uri): Result =
        withContext(Dispatchers.IO) {
            val extractedPhotosDir = File(context.cacheDir, "restore_${UUID.randomUUID()}")
            extractedPhotosDir.mkdirs()
            var dataJson: String? = null

            try {
                context.contentResolver.openInputStream(source)?.use { rawIn ->
                    ZipInputStream(rawIn).use { zip ->
                        var entry: ZipEntry? = zip.nextEntry
                        while (entry != null) {
                            val name = entry.name
                            when {
                                name == DATA_ENTRY -> {
                                    dataJson = zip.readBytes().toString(Charsets.UTF_8)
                                }
                                name.startsWith(PHOTOS_DIR) && !entry.isDirectory -> {
                                    val fileName = name.removePrefix(PHOTOS_DIR)
                                    if (fileName.isNotBlank()) {
                                        val outFile = File(extractedPhotosDir, fileName)
                                        outFile.outputStream().use { out -> zip.copyTo(out) }
                                    }
                                }
                            }
                            zip.closeEntry()
                            entry = zip.nextEntry
                        }
                    }
                } ?: throw java.io.IOException("Could not open backup file for reading")

                val json = dataJson ?: throw java.io.IOException("Backup file is missing ${DATA_ENTRY}")
                val root = JSONObject(json)
                val array = root.getJSONArray("fragrances")

                val entries = mutableListOf<Pair<Fragrance, List<String>>>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val extractedPhotoName = if (obj.isNull("photoFile")) null else obj.optString("photoFile").takeIf { it.isNotBlank() }

                    val storedPhotoName = extractedPhotoName?.let { name ->
                        val extracted = File(extractedPhotosDir, name)
                        if (extracted.exists()) {
                            val newName = "bottle_${UUID.randomUUID()}.jpg"
                            extracted.copyTo(PhotoStore.fileFor(context, newName), overwrite = true)
                            newName
                        } else null
                    }

                    val tags = mutableListOf<String>()
                    val tagsArray = obj.optJSONArray("tags")
                    if (tagsArray != null) {
                        for (t in 0 until tagsArray.length()) tags.add(tagsArray.getString(t))
                    }

                    val fragrance = Fragrance(
                        id = 0L,
                        name = obj.optString("name"),
                        brand = obj.optString("brand"),
                        bottleSizeMl = if (obj.isNull("bottleSizeMl")) null else obj.optInt("bottleSizeMl"),
                        purchaseDateMillis = if (obj.isNull("purchaseDateMillis")) null else obj.optLong("purchaseDateMillis"),
                        batchCode = obj.optString("batchCode"),
                        notes = obj.optString("notes"),
                        photoPath = storedPhotoName
                    )
                    entries.add(fragrance to tags)
                }

                repository.replaceAll(entries)
                Result(entries.size)
            } finally {
                extractedPhotosDir.deleteRecursively()
            }
        }
}
