package com.abosalehg.khizana.data.covers

import android.content.Context
import android.graphics.Bitmap
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * On-disk cover cache under filesDir/covers, one JPEG per book id.
 * Covers are derived data: safe to delete, regenerated on demand.
 */
@Singleton
class CoverStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private val dir = File(context.filesDir, "covers")

    fun fileFor(bookId: String): File = File(dir, "$bookId.jpg")

    /** Atomic save: write to a temp file, then rename over the target. */
    fun save(bookId: String, bitmap: Bitmap): File {
        dir.mkdirs()
        val target = fileFor(bookId)
        val tmp = File(dir, "$bookId.tmp")
        tmp.outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
        }
        if (!tmp.renameTo(target)) {
            tmp.copyTo(target, overwrite = true)
            tmp.delete()
        }
        return target
    }

    fun delete(bookId: String) {
        fileFor(bookId).delete()
    }
}
