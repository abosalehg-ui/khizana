package com.abosalehg.khizana.data.backup

import com.abosalehg.khizana.data.scanner.FileFingerprint
import com.abosalehg.khizana.domain.model.MAX_NOTE_LENGTH
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/** A backup file that is not readable as a Khizana backup of a known version. */
class BackupFormatException(message: String, cause: Throwable? = null) :
    IllegalArgumentException(message, cause)

/**
 * Backup payload: everything user-authored, keyed by content fingerprints so
 * a restore on a fresh install re-attaches metadata as soon as files are
 * rescanned. File paths are intentionally NOT backed up — they are device-
 * specific and the fingerprint makes them recoverable.
 */
data class BackupData(
    val books: List<BackupBook>,
    val topics: List<BackupTopic>,
    val tags: List<BackupTag>,
    val bookTags: List<BackupRef>,
    val excludedFolders: List<String>,
    /** Added in format v2; a v1 file simply has none. */
    val bookmarks: List<BackupBookmark> = emptyList()
)

data class BackupBook(
    val id: String,
    val fileName: String,
    val format: String,
    val title: String,
    val author: String?,
    val topicId: Long?,
    val pageCount: Int,
    val locator: String?,
    val progress: Float,
    val readingDirection: String,
    val isHidden: Boolean,
    val manualOrder: Int,
    val fileSize: Long,
    val addedAt: Long,
    val lastReadAt: Long?
)

data class BackupTopic(val id: Long, val name: String, val order: Int)
data class BackupTag(val id: Long, val name: String)
data class BackupRef(val bookId: String, val tagId: Long)

/**
 * A bookmark, without its local row id: ids are per-device autoincrements, and
 * the pair (bookId, page) already identifies a bookmark uniquely.
 */
data class BackupBookmark(
    val bookId: String,
    val page: Int,
    val note: String?,
    val createdAt: Long
)

/** Pure JSON (de)serialization — no Android, no DAOs, fully unit-testable. */
object BackupSerializer {

    /**
     * v2 added the `bookmarks` section. v1 files still restore — the section
     * is simply absent — but a v2 file is rejected by older builds, which is
     * the point of the check in [fromJson].
     */
    const val FORMAT_VERSION = 2

    /**
     * Largest backup we will read into memory (see BackupManager.importFrom).
     *
     * A real library of several thousand books serializes to two or three
     * megabytes, so 8 MB is generous for anything this app writes. The cap is
     * not about disk: the whole restore runs inside one transaction, and an
     * absurdly large file would hold the database locked while it applied.
     */
    const val MAX_BACKUP_BYTES = 8 * 1024 * 1024

    fun toJson(data: BackupData): String {
        val root = JSONObject()
        root.put("version", FORMAT_VERSION)
        root.put("books", JSONArray().apply {
            data.books.forEach { b ->
                put(JSONObject().apply {
                    put("id", b.id)
                    put("fileName", b.fileName)
                    put("format", b.format)
                    put("title", b.title)
                    putOpt("author", b.author)
                    putOpt("topicId", b.topicId)
                    put("pageCount", b.pageCount)
                    putOpt("locator", b.locator)
                    put("progress", b.progress.toDouble())
                    put("readingDirection", b.readingDirection)
                    put("isHidden", b.isHidden)
                    put("manualOrder", b.manualOrder)
                    put("fileSize", b.fileSize)
                    put("addedAt", b.addedAt)
                    putOpt("lastReadAt", b.lastReadAt)
                })
            }
        })
        root.put("topics", JSONArray().apply {
            data.topics.forEach { t ->
                put(JSONObject().put("id", t.id).put("name", t.name).put("order", t.order))
            }
        })
        root.put("tags", JSONArray().apply {
            data.tags.forEach { t -> put(JSONObject().put("id", t.id).put("name", t.name)) }
        })
        root.put("bookTags", JSONArray().apply {
            data.bookTags.forEach { r ->
                put(JSONObject().put("bookId", r.bookId).put("tagId", r.tagId))
            }
        })
        root.put("excludedFolders", JSONArray(data.excludedFolders))
        root.put("bookmarks", JSONArray().apply {
            data.bookmarks.forEach { b ->
                put(JSONObject().apply {
                    put("bookId", b.bookId)
                    put("page", b.page)
                    putOpt("note", b.note)
                    put("createdAt", b.createdAt)
                })
            }
        })
        return root.toString(2)
    }

    /**
     * A backup file is user-supplied input, not trusted app state: the version
     * is checked, book ids must be bare fingerprints (they end up as file names
     * in the cover store), and numeric fields are clamped to their valid range.
     * Anything malformed raises [BackupFormatException] rather than being
     * half-applied to the user's library.
     */
    fun fromJson(json: String): BackupData {
        val root = try {
            JSONObject(json)
        } catch (e: JSONException) {
            throw BackupFormatException("Not a JSON document", e)
        }
        val version = root.optInt("version", 0)
        if (version <= 0 || version > FORMAT_VERSION) {
            throw BackupFormatException(
                "Unsupported backup version $version (this build reads up to $FORMAT_VERSION)"
            )
        }
        val books = root.optJSONArray("books").orEmpty().mapObjects { o ->
            val id = o.optString("id")
            if (!FileFingerprint.isValidId(id)) {
                throw BackupFormatException("Book id is not a content fingerprint")
            }
            BackupBook(
                id = id,
                fileName = o.optString("fileName"),
                format = o.optString("format", "PDF"),
                title = o.optString("title"),
                author = o.optStringOrNull("author"),
                topicId = if (o.has("topicId") && !o.isNull("topicId")) o.getLong("topicId") else null,
                pageCount = o.optInt("pageCount").coerceAtLeast(0),
                locator = o.optStringOrNull("locator"),
                progress = o.optDouble("progress", 0.0).toFloat().coerceIn(0f, 1f),
                readingDirection = o.optString("readingDirection", "AUTO"),
                isHidden = o.optBoolean("isHidden"),
                manualOrder = o.optInt("manualOrder").coerceAtLeast(0),
                fileSize = o.optLong("fileSize").coerceAtLeast(0),
                addedAt = o.optLong("addedAt"),
                lastReadAt = if (o.has("lastReadAt") && !o.isNull("lastReadAt")) o.getLong("lastReadAt") else null
            )
        }
        val topics = root.optJSONArray("topics").orEmpty().mapObjects { o ->
            BackupTopic(o.getLong("id"), o.getString("name"), o.optInt("order"))
        }
        val tags = root.optJSONArray("tags").orEmpty().mapObjects { o ->
            BackupTag(o.getLong("id"), o.getString("name"))
        }
        val refs = root.optJSONArray("bookTags").orEmpty().mapObjects { o ->
            val bookId = o.optString("bookId")
            // Same rule as books and bookmarks. `book_tags` has no foreign key,
            // so a reference to a book that cannot exist would sit there for
            // good — and `pruneUnused` keeps a tag alive for exactly that kind
            // of row, leaving a tag in the filter row with no book behind it.
            if (!FileFingerprint.isValidId(bookId)) {
                throw BackupFormatException("Tag reference book id is not a content fingerprint")
            }
            BackupRef(bookId, o.getLong("tagId"))
        }
        val excluded = root.optJSONArray("excludedFolders").orEmpty().let { arr ->
            (0 until arr.length()).map { arr.getString(it) }
        }.filter(::isUsableExclusion)
        val bookmarks = root.optJSONArray("bookmarks").orEmpty().mapObjects { o ->
            val bookId = o.optString("bookId")
            // Same rule as book ids: it is a fingerprint or it is not ours.
            if (!FileFingerprint.isValidId(bookId)) {
                throw BackupFormatException("Bookmark book id is not a content fingerprint")
            }
            BackupBookmark(
                bookId = bookId,
                page = o.optInt("page").coerceAtLeast(0),
                note = o.optStringOrNull("note")?.take(MAX_NOTE_LENGTH),
                createdAt = o.optLong("createdAt").coerceAtLeast(0)
            )
        }
        return BackupData(books, topics, tags, refs, excluded, bookmarks)
    }

    /**
     * An exclusion is only meaningful as an absolute path to a real folder.
     *
     * A blank entry — or one made of nothing but slashes — collapses to the
     * empty prefix, which matches every absolute path on the device: one such
     * row in a hand-edited backup would hide the reader's whole library behind
     * a scan that "succeeded" with zero files. Dropping it here is quieter than
     * rejecting the file, because the entry carries no information to lose.
     */
    private fun isUsableExclusion(path: String): Boolean =
        path.startsWith("/") && path.trimEnd('/').isNotEmpty()

    private fun JSONArray?.orEmpty(): JSONArray = this ?: JSONArray()

    private fun <T> JSONArray.mapObjects(transform: (JSONObject) -> T): List<T> =
        (0 until length()).map { transform(getJSONObject(it)) }

    private fun JSONObject.optStringOrNull(key: String): String? =
        if (has(key) && !isNull(key)) getString(key) else null
}
