package com.abosalehg.khizana

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abosalehg.khizana.data.settings.SettingsRepository
import com.abosalehg.khizana.domain.model.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Owns nothing but the app-wide theme, which the root composable needs before
 * any screen exists. Changing the theme lives in `SettingsViewModel`.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    settingsRepository: SettingsRepository
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = settingsRepository.themeMode
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ThemeMode.SYSTEM
        )
}
