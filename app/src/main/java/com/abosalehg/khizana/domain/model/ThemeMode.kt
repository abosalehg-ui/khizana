package com.abosalehg.khizana.domain.model

import com.abosalehg.khizana.util.enumOrNull

/** App-wide theme preference, persisted in DataStore. */
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK;

    companion object {
        fun fromName(name: String?): ThemeMode = enumOrNull<ThemeMode>(name) ?: SYSTEM
    }
}
