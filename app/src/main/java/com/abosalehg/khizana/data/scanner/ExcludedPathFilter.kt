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
            path == normalized || path.startsWith("$normalized/")
        }
}
