package com.abosalehg.khizana.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.abosalehg.khizana.domain.model.ShelfSort
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

    /** Within-shelf ordering; applies to every shelf at once. */
    val shelfSort: Flow<ShelfSort> = dataStore.data
        .map { prefs -> ShelfSort.fromName(prefs[Keys.SHELF_SORT]) }

    suspend fun setShelfSort(sort: ShelfSort) {
        dataStore.edit { prefs -> prefs[Keys.SHELF_SORT] = sort.name }
    }

    /** Deep scan walks all of external storage instead of just MediaStore. */
    val deepScanEnabled: Flow<Boolean> = dataStore.data
        .map { prefs -> prefs[Keys.DEEP_SCAN] ?: false }

    suspend fun setDeepScanEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[Keys.DEEP_SCAN] = enabled }
    }

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DEEP_SCAN = booleanPreferencesKey("deep_scan")
        val SHELF_SORT = stringPreferencesKey("shelf_sort")
    }
}
