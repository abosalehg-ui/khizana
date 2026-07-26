package com.abosalehg.khizana.data.backup

import org.json.JSONArray
import org.json.JSONObject

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
    val excludedFolders: List<String>
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

/** Pure JSON (de)serialization — no Android, no DAOs, fully unit-testable. */
object BackupSerializer {

    const val FORMAT_VERSION = 1

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
        return root.toString(2)
    }

    fun fromJson(json: String): BackupData {
        val root = JSONObject(json)
        val books = root.optJSONArray("books").orEmpty().mapObjects { o ->
            BackupBook(
                id = o.getString("id"),
                fileName = o.optString("fileName"),
                format = o.optString("format", "PDF"),
                title = o.optString("title"),
                author = o.optStringOrNull("author"),
                topicId = if (o.has("topicId") && !o.isNull("topicId")) o.getLong("topicId") else null,
                pageCount = o.optInt("pageCount"),
                locator = o.optStringOrNull("locator"),
                progress = o.optDouble("progress", 0.0).toFloat(),
                readingDirection = o.optString("readingDirection", "AUTO"),
                isHidden = o.optBoolean("isHidden"),
                manualOrder = o.optInt("manualOrder"),
                fileSize = o.optLong("fileSize"),
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
            BackupRef(o.getString("bookId"), o.getLong("tagId"))
        }
        val excluded = root.optJSONArray("excludedFolders").orEmpty().let { arr ->
            (0 until arr.length()).map { arr.getString(it) }
        }
        return BackupData(books, topics, tags, refs, excluded)
    }

    private fun JSONArray?.orEmpty(): JSONArray = this ?: JSONArray()

    private fun <T> JSONArray.mapObjects(transform: (JSONObject) -> T): List<T> =
        (0 until length()).map { transform(getJSONObject(it)) }

    private fun JSONObject.optStringOrNull(key: String): String? =
        if (has(key) && !isNull(key)) getString(key) else null
}
