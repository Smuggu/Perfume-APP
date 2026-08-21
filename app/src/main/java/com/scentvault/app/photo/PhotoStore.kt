package com.scentvault.app.photo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/** Stores bottle photos as JPEGs in app-private internal storage, decoupled from any content Uri. */
object PhotoStore {
    private const val MAX_DIMENSION = 1600
    private const val JPEG_QUALITY = 87

    fun photosDir(context: Context): File =
        File(context.filesDir, "photos").apply { mkdirs() }

    private fun cameraDir(context: Context): File =
        File(context.cacheDir, "camera").apply { mkdirs() }

    /** Creates a blank temp file + content Uri for a camera capture Intent to write into. */
    fun createCameraCaptureUri(context: Context): Pair<File, Uri> {
        val file = File(cameraDir(context), "capture_${UUID.randomUUID()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        return file to uri
    }

    /**
     * Decodes the image at [source], downsamples it, corrects EXIF rotation, and writes a fresh
     * JPEG into internal storage. Returns the stored file's name (relative to [photosDir]).
     */
    fun importImage(context: Context, source: Uri): String? {
        val resolver = context.contentResolver

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        // decodeStream always returns null in inJustDecodeBounds mode, so the stream-open
        // check has to happen separately rather than on decodeStream's own return value.
        val stream = resolver.openInputStream(source) ?: return null
        stream.use { input -> BitmapFactory.decodeStream(input, null, bounds) }

        val sampleSize = calculateSampleSize(bounds.outWidth, bounds.outHeight, MAX_DIMENSION)
        val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val bitmap = resolver.openInputStream(source)?.use {
            BitmapFactory.decodeStream(it, null, decodeOptions)
        } ?: return null

        val rotationDegrees = resolver.openInputStream(source)?.use { readExifRotation(it) } ?: 0
        val oriented = if (rotationDegrees != 0) rotate(bitmap, rotationDegrees) else bitmap

        val fileName = "bottle_${UUID.randomUUID()}.jpg"
        val outFile = File(photosDir(context), fileName)
        FileOutputStream(outFile).use { out ->
            oriented.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
        }
        if (oriented !== bitmap) bitmap.recycle()
        oriented.recycle()

        return fileName
    }

    fun fileFor(context: Context, relativeName: String): File = File(photosDir(context), relativeName)

    fun delete(context: Context, relativeName: String?) {
        if (relativeName.isNullOrBlank()) return
        fileFor(context, relativeName).delete()
    }

    private fun calculateSampleSize(width: Int, height: Int, maxDimension: Int): Int {
        var sampleSize = 1
        var w = width
        var h = height
        while (w / 2 >= maxDimension || h / 2 >= maxDimension) {
            w /= 2
            h /= 2
            sampleSize *= 2
        }
        return sampleSize
    }

    private fun readExifRotation(input: java.io.InputStream): Int {
        return try {
            val exif = ExifInterface(input)
            when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        } catch (_: Exception) {
            0
        }
    }

    private fun rotate(bitmap: Bitmap, degrees: Int): Bitmap {
        val matrix = android.graphics.Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
