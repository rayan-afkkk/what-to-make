package com.whattocook.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whattocook.app.data.auth.AuthRepository
import com.whattocook.app.data.settings.SettingsRepository
import com.whattocook.app.domain.model.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AppUiState(
    val themeMode: ThemeMode = ThemeMode.DEFAULT,
    /** null while DataStore is loading. */
    val onboardingCompleted: Boolean? = null,
)

@HiltViewModel
class AppViewModel @Inject constructor(
    private val settings: SettingsRepository,
    authRepository: AuthRepository,
) : ViewModel() {

    val uiState: StateFlow<AppUiState> = combine(
        settings.themeMode,
        settings.onboardingCompleted,
    ) { theme, onboarded -> AppUiState(theme, onboarded) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppUiState())

    init {
        // Start the anonymous Firebase session early so the first request has a token.
        viewModelScope.launch { authRepository.ensureSignedIn() }
    }

    fun completeOnboarding() {
        viewModelScope.launch { settings.setOnboardingCompleted(true) }
    }
}
