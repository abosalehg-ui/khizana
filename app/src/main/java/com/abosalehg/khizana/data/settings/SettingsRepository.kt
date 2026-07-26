package com.abosalehg.khizana.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.abosalehg.khizana.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** Single source of truth for user preferences, backed by Preferences DataStore. */
@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {

    val themeMode: Flow<ThemeMode> = dataStore.data
        .map { prefs -> ThemeMode.fromName(prefs[Keys.THEME_MODE]) }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { prefs -> prefs[Keys.THEME_MODE] = mode.name }
    }

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
    }
}
