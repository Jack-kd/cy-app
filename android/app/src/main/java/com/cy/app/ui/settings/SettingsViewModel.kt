package com.cy.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cy.app.data.AppSettings
import com.cy.app.data.SettingsRepository
import com.cy.app.data.model.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val repo: SettingsRepository) : ViewModel() {

    val settings: StateFlow<AppSettings> = repo.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppSettings(),
    )

    fun selectProvider(id: String) = viewModelScope.launch { repo.setSelectedProvider(id) }
    fun setApiKey(providerId: String, key: String) = viewModelScope.launch { repo.setApiKey(providerId, key) }
    fun setBaseUrl(providerId: String, url: String) = viewModelScope.launch { repo.setBaseUrl(providerId, url) }
    fun setModel(providerId: String, model: String) = viewModelScope.launch { repo.setModel(providerId, model) }
    fun setDeepThink(enabled: Boolean) = viewModelScope.launch { repo.setDeepThink(enabled) }
    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { repo.setThemeMode(mode) }
}