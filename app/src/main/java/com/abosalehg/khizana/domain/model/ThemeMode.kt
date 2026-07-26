package com.abosalehg.khizana.domain.model

/** App-wide theme preference, persisted in DataStore. */
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK;

    companion object {
        fun fromName(name: String?): ThemeMode =
            entries.firstOrNull { it.name == name } ?: SYSTEM
    }
}
