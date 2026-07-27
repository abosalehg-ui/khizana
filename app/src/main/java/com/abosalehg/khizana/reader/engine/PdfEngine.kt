package com.abosalehg.khizana.reader.engine

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.Log
import java.io.File
import java.io.IOException

/**
 * PDF engine backed by the platform's [PdfRenderer].
 *
 * Khizana deliberately uses the OS renderer rather than bundling a native PDF
 * parser: PDF parsing is a memory-unsafe attack surface fed by untrusted files
 * from shared storage, inside a process that holds All Files Access. The
 * platform renderer is patched through Google Play system updates; a vendored
 * copy of Pdfium is frozen at whatever it was on the day it was published.
 *
 * [PdfRenderer] allows only one open page at a time and is not thread-safe —
 * both are guaranteed by the single engine thread in `ReaderViewModel` and by
 * closing each page inside [renderPage].
 */
class PdfEngine private constructor(
    private val descriptor: ParcelFileDescriptor,
    private val renderer: PdfRenderer
) : BookEngine {

    override val pageCount: Int = renderer.pageCount

    override fun renderPage(index: Int, targetWidth: Int): Bitmap? {
        if (index < 0 || index >= pageCount) return null
        return try {
            renderer.openPage(index).use { page ->
                if (page.width <= 0 || page.height <= 0) return null
                val width = targetWidth.coerceAtLeast(1)
                val height = (width.toLong() * page.height / page.width)
                    .toInt()
                    .coerceAtLeast(1)
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                // The renderer composites onto the bitmap without clearing it,
                // and PDF pages expect paper rather than transparency.
                bitmap.eraseColor(Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                bitmap
            }
        } catch (e: OutOfMemoryError) {
            // A page that asks for more pixels than we have is a page we skip,
            // not a crash: Error escapes `catch (Exception)`.
            Log.w(TAG, "Out of memory rendering page $index at ${targetWidth}px", e)
            null
        } catch (e: Exception) {
            Log.w(TAG, "Failed to render page $index", e)
            null
        }
    }

    override fun close() {
        runCatching { renderer.close() }
            .onFailure { Log.w(TAG, "Failed to close PDF renderer", it) }
        runCatching { descriptor.close() }
            .onFailure { Log.w(TAG, "Failed to close PDF descriptor", it) }
    }

    companion object {
        private const val TAG = "PdfEngine"

        fun open(file: File): EngineOpenResult {
            val fd = try {
                ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            } catch (e: Exception) {
                Log.w(TAG, "Cannot open ${file.name} for reading", e)
                return EngineOpenResult.Corrupt
            }
            return try {
                EngineOpenResult.Success(PdfEngine(fd, PdfRenderer(fd)))
            } catch (e: SecurityException) {
                // Documented contract: PdfRenderer throws SecurityException for
                // a password-protected document.
                runCatching { fd.close() }
                EngineOpenResult.Protected
            } catch (e: IOException) {
                Log.w(TAG, "Cannot parse ${file.name} as PDF", e)
                runCatching { fd.close() }
                EngineOpenResult.Corrupt
            } catch (e: Exception) {
                Log.w(TAG, "Unexpected failure opening ${file.name}", e)
                runCatching { fd.close() }
                EngineOpenResult.Corrupt
            }
        }
    }
}
