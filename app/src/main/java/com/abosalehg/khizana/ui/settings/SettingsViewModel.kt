package com.abosalehg.khizana.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abosalehg.khizana.data.backup.BackupRunner
import com.abosalehg.khizana.data.repo.LibraryRepository
import com.abosalehg.khizana.data.settings.SettingsRepository
import com.abosalehg.khizana.domain.model.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val libraryRepository: LibraryRepository,
    private val backupRunner: BackupRunner
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = settingsRepository.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeMode.SYSTEM)

    val deepScanEnabled: StateFlow<Boolean> = settingsRepository.deepScanEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val excludedFolders: StateFlow<List<String>> = libraryRepository.excludedFolders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** True while an export or import is running, so the buttons can disable. */
    val backupRunning: StateFlow<Boolean> = backupRunner.running

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
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

    /**
     * Both run in the application scope, not [viewModelScope]: leaving
     * Settings mid-restore used to cancel the restore and roll it back without
     * a word.
     */
    fun backup(uri: Uri, onResult: (Boolean) -> Unit) = backupRunner.export(uri, onResult)

    fun restore(uri: Uri, onResult: (Boolean) -> Unit) = backupRunner.restore(uri, onResult)
}
