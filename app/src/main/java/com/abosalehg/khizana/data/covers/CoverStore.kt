package com.abosalehg.khizana.data.covers

import android.graphics.Bitmap
import android.util.Log
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * On-disk cover cache under filesDir/covers, one JPEG per book id.
 * Covers are derived data: safe to delete, regenerated on demand.
 *
 * The directory is injected rather than derived from a Context so the store is
 * usable from JVM unit tests against a temporary folder.
 */
@Singleton
class CoverStore @Inject constructor(
    @CoversDir private val dir: File
) {

    /**
     * Book ids are SHA-256 hex fingerprints, but ids also arrive from restored
     * backup files, which are user-supplied. Rejecting anything that is not a
     * bare fingerprint keeps `../` out of the path we are about to build.
     */
    fun fileFor(bookId: String): File? =
        if (FINGERPRINT.matches(bookId)) File(dir, "$bookId.jpg") else null

    /** Atomic save: write to a temp file, then rename over the target. */
    fun save(bookId: String, bitmap: Bitmap): File? {
        val target = fileFor(bookId) ?: run {
            Log.w(TAG, "Refusing to save a cover for a non-fingerprint id")
            return null
        }
        return try {
            dir.mkdirs()
            val tmp = File(dir, "$bookId.tmp")
            tmp.outputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            if (!tmp.renameTo(target)) {
                tmp.copyTo(target, overwrite = true)
                tmp.delete()
            }
            target
        } catch (e: Exception) {
            Log.w(TAG, "Failed to write cover for $bookId", e)
            null
        }
    }

    fun delete(bookId: String) {
        fileFor(bookId)?.delete()
    }

    private companion object {
        const val TAG = "CoverStore"
        val FINGERPRINT = Regex("^[0-9a-f]{64}$")
    }
}
