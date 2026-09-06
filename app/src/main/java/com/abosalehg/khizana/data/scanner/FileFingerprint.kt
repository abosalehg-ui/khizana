package com.abosalehg.khizana.data.scanner

import java.io.File
import java.security.MessageDigest

/**
 * Book identity: SHA-256(fileSize.toString() + first 64 KB of content), hex-encoded.
 *
 * Moving or renaming a file keeps its id, so reading progress, bookmarks and
 * hidden state all survive. On rescan, a known id at a new path means
 * "update the path only", never a new row.
 */
object FileFingerprint {

    const val HEAD_SIZE_BYTES: Int = 64 * 1024

    /** What [compute] produces: 64 lowercase hex characters, nothing else. */
    private val ID_PATTERN = Regex("^[0-9a-f]{64}$")

    /**
     * True when [id] is a bare fingerprint this object could have produced.
     *
     * Ids do not only come from [compute]: they also arrive from backup files,
     * which are user-supplied, and they end up as file names in the cover
     * store. The rule lives here — at the source of the format — because it is
     * a security boundary, and a boundary written down twice is a boundary
     * that will one day be updated once.
     */
    fun isValidId(id: String?): Boolean = id != null && ID_PATTERN.matches(id)

    fun compute(file: File): String {
        val head = ByteArray(HEAD_SIZE_BYTES)
        val read = file.inputStream().use { input ->
            var offset = 0
            while (offset < head.size) {
                val r = input.read(head, offset, head.size - offset)
                if (r < 0) break
                offset += r
            }
            offset
        }
        return compute(file.length(), head, read)
    }

    /** Pure core, unit-testable without touching the filesystem. */
    fun compute(fileSize: Long, head: ByteArray, headLength: Int = head.size): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(fileSize.toString().toByteArray(Charsets.UTF_8))
        digest.update(head, 0, headLength)
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
