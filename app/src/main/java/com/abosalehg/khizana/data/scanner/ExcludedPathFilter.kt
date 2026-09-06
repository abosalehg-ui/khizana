package com.abosalehg.khizana.data.scanner

/**
 * Decides whether a file path falls under any excluded folder.
 * A path is excluded when it IS the folder or lives anywhere under it.
 * Similar-named siblings must NOT match ("/Books" must not exclude "/BooksOld").
 */
object ExcludedPathFilter {

    fun isExcluded(path: String, excludedFolders: Collection<String>): Boolean =
        excludedFolders.any { folder ->
            val normalized = folder.trimEnd('/')
            // An empty entry — "", "/", "///" — would leave the prefix test as
            // startsWith("/"), which every absolute path on the device passes:
            // one blank row would silently exclude the entire library and the
            // scan would "succeed" with zero files. Such a row is never a real
            // exclusion, so it matches nothing.
            if (normalized.isEmpty()) return@any false
            path == normalized || path.startsWith("$normalized/")
        }
}
