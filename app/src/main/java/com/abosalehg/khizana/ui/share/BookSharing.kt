package com.abosalehg.khizana.ui.share

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.FileProvider
import com.abosalehg.khizana.R
import com.abosalehg.khizana.domain.model.Book
import com.abosalehg.khizana.domain.model.BookFormat
import java.io.File

/**
 * Hands a book file to another app through the system share sheet.
 *
 * The file travels as a `content://` URI minted by a FileProvider, never as
 * the `file://` path the library stores: file URIs crossing a process boundary
 * have thrown `FileUriExposedException` since Android 7, and the provider
 * grants read access to exactly one file, to exactly the app the user picks,
 * for as long as that app's task lives.
 *
 * The app still holds no INTERNET permission — it hands the file to the app
 * the user chooses and nothing else. Where it goes from there is their call.
 */
object BookSharing {

    /** Must match the authority of the provider declared in the manifest. */
    private const val AUTHORITY_SUFFIX = ".fileprovider"

    private const val TAG = "BookSharing"

    fun mimeTypeFor(format: BookFormat): String = when (format) {
        BookFormat.PDF -> "application/pdf"
        BookFormat.CBZ -> "application/vnd.comicbook+zip"
        BookFormat.EPUB -> "application/epub+zip"
    }

    /**
     * Returns false when there was nothing to share — a MISSING book carries an
     * empty path, and any file can vanish between the last scan and this tap —
     * so the caller can say so instead of opening an empty share sheet.
     */
    fun share(context: Context, book: Book): Boolean {
        val file = File(book.path)
        if (book.path.isEmpty() || !file.isFile) {
            Log.w(TAG, "Refusing to share ${book.id}: no readable file at its path")
            return false
        }
        val uri = try {
            FileProvider.getUriForFile(context, context.packageName + AUTHORITY_SUFFIX, file)
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "FileProvider cannot expose the path of ${book.id}", e)
            return false
        }
        val send = Intent(Intent.ACTION_SEND).apply {
            type = mimeTypeFor(book.format)
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TITLE, book.title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return try {
            context.startActivity(
                Intent.createChooser(send, context.getString(R.string.share_book))
            )
            true
        } catch (e: ActivityNotFoundException) {
            Log.w(TAG, "No app on this device accepts a share", e)
            false
        }
    }
}
