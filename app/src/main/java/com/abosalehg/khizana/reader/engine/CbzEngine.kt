package com.abosalehg.khizana.reader.engine

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.abosalehg.khizana.data.covers.CbzCover
import java.io.File
import java.util.zip.ZipFile

/**
 * CBZ engine: pages are the archive's image entries in reading order.
 * An archive that can't be opened, or contains no images, is CORRUPT.
 */
class CbzEngine private constructor(
    private val zip: ZipFile,
    private val pages: List<String>
) : BookEngine {

    override val pageCount: Int = pages.size

    override fun renderPage(index: Int, targetWidth: Int): Bitmap? {
        val name = pages.getOrNull(index) ?: return null
        return try {
            val entry = zip.getEntry(name) ?: return null
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            zip.getInputStream(entry).use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0) return null
            val options = BitmapFactory.Options().apply {
                inSampleSize = CbzCover.sampleSize(
                    bounds.outWidth,
                    bounds.outHeight,
                    targetWidth.coerceAtLeast(1)
                )
            }
            zip.getInputStream(entry).use { BitmapFactory.decodeStream(it, null, options) }
        } catch (e: OutOfMemoryError) {
            // A decompression bomb is a page we skip, not a crash: Error is not
            // an Exception and would otherwise take the whole process down.
            Log.w(TAG, "Out of memory decoding entry $name", e)
            null
        } catch (e: Exception) {
            Log.w(TAG, "Failed to decode entry $name", e)
            null
        }
    }

    override fun close() {
        runCatching { zip.close() }
            .onFailure { Log.w(TAG, "Failed to close archive", it) }
    }

    companion object {
        private const val TAG = "CbzEngine"

        fun open(file: File): EngineOpenResult = try {
            val zip = ZipFile(file)
            val pages = CbzCover.sortedImageEntries(
                zip.entries().asSequence().map { it.name }.toList()
            )
            if (pages.isEmpty()) {
                zip.close()
                Log.w(TAG, "${file.name} holds no decodable image entries")
                EngineOpenResult.Corrupt
            } else {
                EngineOpenResult.Success(CbzEngine(zip, pages))
            }
        } catch (e: Exception) {
            Log.w(TAG, "Cannot open ${file.name} as a CBZ archive", e)
            EngineOpenResult.Corrupt
        }
    }
}
