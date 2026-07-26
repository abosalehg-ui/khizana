package com.abosalehg.khizana

import com.abosalehg.khizana.data.scanner.TreePaths
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TreePathsTest {

    private val primary = "/storage/emulated/0"

    @Test
    fun `primary volume folder maps under the primary root`() {
        assertEquals(
            "/storage/emulated/0/Books",
            TreePaths.pathForDocId("primary:Books", primary)
        )
    }

    @Test
    fun `nested primary path is preserved`() {
        assertEquals(
            "/storage/emulated/0/Documents/Archive",
            TreePaths.pathForDocId("primary:Documents/Archive", primary)
        )
    }

    @Test
    fun `primary root itself maps to the root`() {
        assertEquals(primary, TreePaths.pathForDocId("primary:", primary))
    }

    @Test
    fun `sd card volume maps under storage`() {
        assertEquals(
            "/storage/1234-ABCD/Comics",
            TreePaths.pathForDocId("1234-ABCD:Comics", primary)
        )
    }

    @Test
    fun `unknown providers are rejected`() {
        assertNull(TreePaths.pathForDocId("downloads:12345", primary))
        assertNull(TreePaths.pathForDocId("com.cloud.provider:doc", primary))
    }
}
