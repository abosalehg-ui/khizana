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
