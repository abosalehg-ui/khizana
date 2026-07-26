package com.abosalehg.khizana.reader.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.os.ParcelFileDescriptor
import com.shockwave.pdfium.PdfDocument
import com.shockwave.pdfium.PdfPasswordException
import com.shockwave.pdfium.PdfiumCore
import java.io.File

/** Pdfium-backed PDF engine. */
class PdfEngine private constructor(
    private val core: PdfiumCore,
    private val document: PdfDocument
) : BookEngine {

    override val pageCount: Int
        get() = core.getPageCount(document)

    private val openedPages = mutableSetOf<Int>()

    override fun renderPage(index: Int, targetWidth: Int): Bitmap? {
        if (index < 0 || index >= pageCount) return null
        return try {
            if (openedPages.add(index)) core.openPage(document, index)
            val pageWidth = core.getPageWidthPoint(document, index)
            val pageHeight = core.getPageHeightPoint(document, index)
            if (pageWidth <= 0 || pageHeight <= 0) return null
            val width = targetWidth.coerceAtLeast(1)
            val height = (width.toLong() * pageHeight / pageWidth).toInt().coerceAtLeast(1)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            // Pdfium leaves untouched pixels transparent; pages expect paper.
            bitmap.eraseColor(Color.WHITE)
            core.renderPageBitmap(document, bitmap, index, 0, 0, width, height)
            bitmap
        } catch (e: Exception) {
            null
        }
    }

    override fun close() {
        runCatching { core.closeDocument(document) }
    }

    companion object {
        fun open(context: Context, file: File, password: String? = null): EngineOpenResult {
            val fd = try {
                ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            } catch (e: Exception) {
                return EngineOpenResult.Corrupt
            }
            val core = PdfiumCore(context)
            return try {
                val document =
                    if (password != null) core.newDocument(fd, password)
                    else core.newDocument(fd)
                EngineOpenResult.Success(PdfEngine(core, document))
            } catch (e: PdfPasswordException) {
                runCatching { fd.close() }
                EngineOpenResult.Protected
            } catch (e: Exception) {
                runCatching { fd.close() }
                EngineOpenResult.Corrupt
            }
        }
    }
}
