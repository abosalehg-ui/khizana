package com.abosalehg.khizana.data.scanner

/**
 * Converts a SAF tree document id (from ACTION_OPEN_DOCUMENT_TREE) to a
 * filesystem path. Works for the primary volume and SD-card style volumes;
 * returns null for providers we can't map (downloads provider, cloud docs).
 */
object TreePaths {

    private val VOLUME_ID = Regex("[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}")

    fun pathForDocId(docId: String, primaryRoot: String): String? {
        val parts = docId.split(":", limit = 2)
        val volume = parts[0]
        val relative = parts.getOrElse(1) { "" }.trimEnd('/')
        return when {
            volume == "primary" ->
                if (relative.isEmpty()) primaryRoot else "$primaryRoot/$relative"
            VOLUME_ID.matches(volume) ->
                if (relative.isEmpty()) "/storage/$volume" else "/storage/$volume/$relative"
            else -> null
        }
    }
}
