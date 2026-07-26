package com.abosalehg.khizana.data.covers

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.util.zip.ZipFile

/**
 * Lightweight CBZ helpers for cover extraction: the cover is the first image
 * entry in alphabetical order. (The full CBZ reading engine, with natural
 * ordering, arrives in a later milestone.)
 */
object CbzCover {

    private val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp", "gif", "bmp")

    /** Entry-name predicate, pure and unit-testable. */
    fun isImageEntry(name: String): Boolean {
        if (name.endsWith("/")) return false
        if (name.startsWith("__MACOSX/")) return false
        val fileName = name.substringAfterLast('/')
        if (fileName.startsWith(".")) return false
        return fileName.substringAfterLast('.', "").lowercase() in IMAGE_EXTENSIONS
    }

    /**
     * Image entries in reading order (case-insensitive alphabetical for now;
     * natural ordering with numbers arrives in a later milestone).
     */
    fun sortedImageEntries(entryNames: List<String>): List<String> =
        entryNames.filter(::isImageEntry).sortedBy { it.lowercase() }

    /** Picks the cover entry from a list of archive entry names. */
    fun pickCoverEntry(entryNames: List<String>): String? =
        sortedImageEntries(entryNames).firstOrNull()

    fun countImages(entryNames: List<String>): Int = entryNames.count(::isImageEntry)

    /**
     * Opens the archive and decodes its cover downsampled to roughly
     * [targetWidth]. Returns the bitmap plus the archive's image count,
     * or null when the archive has no decodable images.
     * Throws on an unreadable/corrupt archive — callers map that to CORRUPT.
     */
    fun extract(file: File, targetWidth: Int): Extraction? = ZipFile(file).use { zip ->
        val names = zip.entries().asSequence().map { it.name }.toList()
        val coverName = pickCoverEntry(names) ?: return null
        val entry = zip.getEntry(coverName) ?: return null

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        zip.getInputStream(entry).use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0) return null

        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize(bounds.outWidth, targetWidth)
        }
        val bitmap = zip.getInputStream(entry).use {
            BitmapFactory.decodeStream(it, null, options)
        } ?: return null
        Extraction(bitmap, countImages(names))
    }

    data class Extraction(val cover: Bitmap, val imageCount: Int)

    /** Largest power of two keeping the decoded width at or above the target. */
    fun sampleSize(sourceWidth: Int, targetWidth: Int): Int {
        var sample = 1
        while (sourceWidth / (sample * 2) >= targetWidth) sample *= 2
        return sample
    }
}
