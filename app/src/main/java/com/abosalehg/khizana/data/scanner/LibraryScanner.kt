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
 * What a scan saw, and how far that can be trusted.
 *
 * A file the scan did not see is marked MISSING — which is only right when the
 * scan could have seen it. [complete] is false when the listing itself failed
 * part-way (a MediaStore query that threw), and then absence proves nothing.
 * [unreadableDirs] are folders a deep walk could not list: books under them
 * are neither found nor lost.
 */
data class ScanResult(
    val files: List<ScannedFile>,
    val complete: Boolean = true,
    val unreadableDirs: List<String> = emptyList()
)

/**
 * Finds book files on the device. An interface so the rescan contract in
 * `LibraryRepository` can be unit tested against a fake source of files
 * instead of a real MediaStore.
 */
interface LibraryScanner {
    fun scan(deep: Boolean, excludedFolders: List<String>): ScanResult
}

/**
 * The real scanner. The fast path queries MediaStore by extension; the
 * optional deep scan walks external storage recursively. Scanning is always
 * manual — never triggered automatically at startup.
 */
class MediaStoreLibraryScanner(
    private val context: Context
) : LibraryScanner {

    override fun scan(deep: Boolean, excludedFolders: List<String>): ScanResult {
        val result = if (deep) scanDeep(excludedFolders) else scanMediaStore(excludedFolders)
        // Deduplicate by absolute path (MediaStore can hold stale duplicates).
        return result.copy(files = result.files.distinctBy { it.file.absolutePath })
    }

    private fun scanMediaStore(excludedFolders: List<String>): ScanResult {
        val results = mutableListOf<ScannedFile>()
        var complete = false
        val uri = MediaStore.Files.getContentUri("external")
        @Suppress("DEPRECATION")
        val dataColumn = MediaStore.Files.FileColumns.DATA
        val selection = "$dataColumn LIKE ? OR $dataColumn LIKE ?"
        val args = arrayOf("%.pdf", "%.cbz")

        try {
            // Sorted, so the same library always comes back in the same order:
            // with two copies of one book, which copy a rescan meets first
            // must not change from one scan to the next.
            val cursor = context.contentResolver
                .query(uri, arrayOf(dataColumn), selection, args, "$dataColumn ASC")
            if (cursor == null) {
                Log.w(TAG, "MediaStore returned no cursor")
            } else {
                cursor.use {
                    val dataIndex = it.getColumnIndexOrThrow(dataColumn)
                    while (it.moveToNext()) {
                        val path = it.getString(dataIndex) ?: continue
                        addCandidate(File(path), excludedFolders, results)
                    }
                }
                complete = true
            }
        } catch (e: Exception) {
            // What was read so far is still real and gets added, but the scan
            // is reported incomplete: the rescan must not read the books it
            // never reached as books that are gone.
            Log.w(TAG, "MediaStore query failed part-way", e)
        }
        return ScanResult(results, complete = complete)
    }

    private fun scanDeep(excludedFolders: List<String>): ScanResult {
        val results = mutableListOf<ScannedFile>()
        val unreadable = mutableListOf<String>()
        val root = Environment.getExternalStorageDirectory()
        root.walkTopDown()
            .onEnter { dir ->
                !dir.name.startsWith(".") &&
                    !isSystemRestrictedDir(dir) &&
                    !ExcludedPathFilter.isExcluded(dir.absolutePath, excludedFolders)
            }
            // A folder that cannot be listed is skipped by the walk without a
            // word; remember it, so its books are not taken for missing.
            .onFail { dir, e ->
                Log.w(TAG, "Cannot list ${dir.absolutePath}", e)
                unreadable += dir.absolutePath
            }
            .filter { it.isFile }
            .forEach { addCandidate(it, excludedFolders, results) }
        return ScanResult(results, unreadableDirs = unreadable)
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
