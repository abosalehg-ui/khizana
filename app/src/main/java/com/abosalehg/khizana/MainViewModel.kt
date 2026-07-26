package com.abosalehg.khizana

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abosalehg.khizana.data.settings.SettingsRepository
import com.abosalehg.khizana.domain.model.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = settingsRepository.themeMode
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ThemeMode.SYSTEM
        )

    /** Temporary M0 affordance: cycles SYSTEM → LIGHT → DARK → SYSTEM. */
    fun cycleThemeMode() {
        viewModelScope.launch {
            val current = settingsRepository.themeMode.first()
            val next = ThemeMode.entries[(current.ordinal + 1) % ThemeMode.entries.size]
            settingsRepository.setThemeMode(next)
        }
    }
}
