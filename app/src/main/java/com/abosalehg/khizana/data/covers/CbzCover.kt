package com.abosalehg.khizana.data.covers

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import java.io.File
import java.util.zip.ZipEntry
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
     * Image entries in natural reading order: digit runs compare as numbers,
     * so page_2 comes before page_10 even without zero padding.
     */
    fun sortedImageEntries(entryNames: List<String>): List<String> =
        entryNames.filter(::isImageEntry)
            .sortedWith(com.abosalehg.khizana.util.NaturalOrderComparator)

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
        val bitmap = decodeEntry(zip, entry, targetWidth) ?: return null
        Extraction(bitmap, countImages(names))
    }

    data class Extraction(val cover: Bitmap, val imageCount: Int)

    /**
     * Decodes one archive entry at roughly [targetWidth], or returns null when
     * it cannot be decoded. The single place an entry's pixels are allocated —
     * the cover extractor and the reader engine both come through here, so the
     * area bound in [sampleSize] and the `OutOfMemoryError` catch cannot drift
     * apart between them.
     */
    fun decodeEntry(zip: ZipFile, entry: ZipEntry, targetWidth: Int): Bitmap? = try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        zip.getInputStream(entry).use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0) {
            null
        } else {
            val options = BitmapFactory.Options().apply {
                inSampleSize = sampleSize(
                    bounds.outWidth,
                    bounds.outHeight,
                    targetWidth.coerceAtLeast(1)
                )
            }
            zip.getInputStream(entry).use { BitmapFactory.decodeStream(it, null, options) }
        }
    } catch (e: OutOfMemoryError) {
        // Error is not an Exception: without this a decompression bomb takes
        // the whole process down instead of failing one image.
        Log.w(TAG, "Out of memory decoding entry ${entry.name}", e)
        null
    }

    private const val TAG = "CbzCover"

    /**
     * Largest power of two that keeps the decoded width at or above the target
     * *and* the decoded pixel count within [pixelBudget].
     *
     * The width test alone is not enough. A 10 x 2,000,000 entry — a shape no
     * real page has, and exactly the shape a decompression bomb takes — is
     * already narrower than the target, so the width loop never runs and the
     * decoder is asked for eighty gigabytes. `catch (OutOfMemoryError)` does
     * survive that, but only after a full round of allocation pressure and
     * garbage collection, once per page. The area test refuses the work up
     * front instead of surviving it.
     *
     * [sourceHeight] of 0 (a header the decoder could not read) falls back to
     * the width-only behaviour rather than dividing by nothing.
     */
    fun sampleSize(sourceWidth: Int, sourceHeight: Int, targetWidth: Int): Int {
        if (sourceWidth <= 0 || targetWidth <= 0) return 1
        // Sharpness first: the largest step that still decodes at or above the
        // target width. For a page-shaped entry this is the whole answer.
        var sample = 1
        while (sourceWidth / (sample * 2) >= targetWidth) sample *= 2
        if (sourceHeight <= 0) return sample
        // Then safety, which is allowed to overrule it: an entry whose area is
        // out of all proportion gets downsampled past the target width rather
        // than decoded. A blurry page beats an eighty-gigabyte allocation.
        val budget = pixelBudget(targetWidth)
        while (
            sample < MAX_SAMPLE_SIZE &&
            (sourceWidth.toLong() / sample) * (sourceHeight.toLong() / sample) > budget
        ) {
            sample *= 2
        }
        return sample
    }

    /**
     * Pixels we are willing to decode for a given target width: enough for a
     * very tall page (3x the target's own square) and nowhere near enough for
     * an archive that is lying about what it holds.
     */
    private fun pixelBudget(targetWidth: Int): Long =
        targetWidth.toLong() * targetWidth * 3

    private const val MAX_SAMPLE_SIZE = 1 shl 16
}
