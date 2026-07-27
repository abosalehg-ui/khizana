package com.abosalehg.khizana.util

/**
 * Parses a persisted enum name back to its constant, or null when the name is
 * missing or unknown (an older/newer schema, or a hand-edited backup file).
 * Replaces the per-enum `fromName` bodies that were copied four times.
 */
inline fun <reified T : Enum<T>> enumOrNull(name: String?): T? =
    if (name == null) null else enumValues<T>().firstOrNull { it.name == name }
