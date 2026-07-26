package com.abosalehg.khizana

import com.abosalehg.khizana.data.scanner.FileFingerprint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class FileFingerprintTest {

    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun `same content at two paths yields the same id`() {
        val content = ByteArray(100_000) { (it % 251).toByte() }
        val a = tmp.newFile("folderA-book.pdf").apply { writeBytes(content) }
        val b = tmp.newFile("renamed and moved.pdf").apply { writeBytes(content) }

        assertEquals(FileFingerprint.compute(a), FileFingerprint.compute(b))
    }

    @Test
    fun `different content with the same size yields different ids`() {
        val a = tmp.newFile("a.pdf").apply { writeBytes(ByteArray(4096) { 1 }) }
        val b = tmp.newFile("b.pdf").apply { writeBytes(ByteArray(4096) { 2 }) }

        assertNotEquals(FileFingerprint.compute(a), FileFingerprint.compute(b))
    }

    @Test
    fun `same head but different size yields different ids`() {
        val head = ByteArray(FileFingerprint.HEAD_SIZE_BYTES) { 7 }
        val idSmall = FileFingerprint.compute(head.size.toLong(), head)
        val idLarge = FileFingerprint.compute(head.size.toLong() + 1, head)

        assertNotEquals(idSmall, idLarge)
    }

    @Test
    fun `files smaller than the head window are handled`() {
        val a = tmp.newFile("tiny1.pdf").apply { writeBytes(byteArrayOf(1, 2, 3)) }
        val b = tmp.newFile("tiny2.pdf").apply { writeBytes(byteArrayOf(1, 2, 3)) }

        assertEquals(FileFingerprint.compute(a), FileFingerprint.compute(b))
    }
}
