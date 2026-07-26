package com.abosalehg.khizana.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abosalehg.khizana.data.backup.BackupManager
import com.abosalehg.khizana.data.settings.SettingsRepository
import com.abosalehg.khizana.domain.model.ThemeMode
import com.abosalehg.khizana.domain.repo.LibraryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val libraryRepository: LibraryRepository,
    private val backupManager: BackupManager
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = settingsRepository.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeMode.SYSTEM)

    val deepScanEnabled: StateFlow<Boolean> = settingsRepository.deepScanEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val excludedFolders: StateFlow<List<String>> = libraryRepository.excludedFolders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun cycleThemeMode() {
        viewModelScope.launch {
            val current = settingsRepository.themeMode.first()
            val next = ThemeMode.entries[(current.ordinal + 1) % ThemeMode.entries.size]
            settingsRepository.setThemeMode(next)
        }
    }

    fun setDeepScan(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setDeepScanEnabled(enabled) }
    }

    fun addExcludedFolder(path: String) {
        viewModelScope.launch { libraryRepository.addExcludedFolder(path) }
    }

    fun removeExcludedFolder(path: String) {
        viewModelScope.launch { libraryRepository.removeExcludedFolder(path) }
    }

    fun backup(uri: Uri, onResult: (Boolean) -> Unit) {
        viewModelScope.launch { onResult(backupManager.exportTo(uri)) }
    }

    fun restore(uri: Uri, onResult: (Boolean) -> Unit) {
        viewModelScope.launch { onResult(backupManager.importFrom(uri)) }
    }
}
