package com.abosalehg.khizana.data.scanner

import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.abosalehg.khizana.domain.model.BookFormat
import java.io.File

/** A candidate book file found on disk. */
data class ScannedFile(
    val file: File,
    val format: BookFormat
)

/**
 * Finds book files on the device. An interface so the rescan contract in
 * `LibraryRepository` can be unit tested against a fake source of files
 * instead of a real MediaStore.
 */
interface LibraryScanner {
    fun scan(deep: Boolean, excludedFolders: List<String>): List<ScannedFile>
}

/**
 * The real scanner. The fast path queries MediaStore by extension; the
 * optional deep scan walks external storage recursively. Scanning is always
 * manual — never triggered automatically at startup.
 */
class MediaStoreLibraryScanner(
    private val context: Context
) : LibraryScanner {

    override fun scan(deep: Boolean, excludedFolders: List<String>): List<ScannedFile> {
        val files = if (deep) scanDeep(excludedFolders) else scanMediaStore(excludedFolders)
        // Deduplicate by absolute path (MediaStore can hold stale duplicates).
        return files.distinctBy { it.file.absolutePath }
    }

    private fun scanMediaStore(excludedFolders: List<String>): List<ScannedFile> {
        val results = mutableListOf<ScannedFile>()
        val uri = MediaStore.Files.getContentUri("external")
        @Suppress("DEPRECATION")
        val dataColumn = MediaStore.Files.FileColumns.DATA
        val selection = "$dataColumn LIKE ? OR $dataColumn LIKE ?"
        val args = arrayOf("%.pdf", "%.cbz")

        try {
            context.contentResolver.query(uri, arrayOf(dataColumn), selection, args, null)
                ?.use { cursor ->
                    val dataIndex = cursor.getColumnIndexOrThrow(dataColumn)
                    while (cursor.moveToNext()) {
                        val path = cursor.getString(dataIndex) ?: continue
                        addCandidate(File(path), excludedFolders, results)
                    }
                }
        } catch (e: Exception) {
            Log.w(TAG, "MediaStore query failed; the scan returns what it found so far", e)
        }
        return results
    }

    private fun scanDeep(excludedFolders: List<String>): List<ScannedFile> {
        val results = mutableListOf<ScannedFile>()
        val root = Environment.getExternalStorageDirectory()
        root.walkTopDown()
            .onEnter { dir ->
                !dir.name.startsWith(".") &&
                    !isSystemRestrictedDir(dir) &&
                    !ExcludedPathFilter.isExcluded(dir.absolutePath, excludedFolders)
            }
            .filter { it.isFile }
            .forEach { addCandidate(it, excludedFolders, results) }
        return results
    }

    private fun addCandidate(
        file: File,
        excludedFolders: List<String>,
        results: MutableList<ScannedFile>
    ) {
        val format = BookFormat.fromExtension(file.extension) ?: return
        if (!file.isFile || file.length() == 0L) return
        if (ExcludedPathFilter.isExcluded(file.absolutePath, excludedFolders)) return
        results += ScannedFile(file, format)
    }

    /** Android/data and Android/obb are blocked by the OS on Android 11+. */
    private fun isSystemRestrictedDir(dir: File): Boolean {
        val parent = dir.parentFile?.name ?: return false
        return parent == "Android" && (dir.name == "data" || dir.name == "obb")
    }

    private companion object {
        const val TAG = "LibraryScanner"
    }
}
