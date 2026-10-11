package com.whattocook.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whattocook.app.data.auth.AuthRepository
import com.whattocook.app.data.settings.SettingsRepository
import com.whattocook.app.domain.RecipeText
import com.whattocook.app.domain.model.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    private val _sharedLink = MutableStateFlow<String?>(null)

    /** A link shared into the app, waiting to be opened on the import screen. */
    val sharedLink: StateFlow<String?> = _sharedLink.asStateFlow()

    fun onSharedText(text: String) {
        val link = RecipeText.extractUrl(text) ?: text.trim().takeIf { it.isNotEmpty() } ?: return
        _sharedLink.value = link
    }

    fun onSharedLinkHandled() {
        _sharedLink.value = null
    }

    init {
        // Create the guest session early so the first request has a token.
        viewModelScope.launch { authRepository.ensureSignedIn() }
    }

    fun completeOnboarding() {
        viewModelScope.launch { settings.setOnboardingCompleted(true) }
    }
}
