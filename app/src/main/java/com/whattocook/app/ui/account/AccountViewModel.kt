package com.whattocook.app.ui.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whattocook.app.data.auth.AuthRepository
import com.whattocook.app.data.repository.AccountRepository
import com.whattocook.app.data.repository.UsageRepository
import com.whattocook.app.data.settings.SettingsRepository
import com.whattocook.app.domain.UsageLimit
import com.whattocook.app.domain.UsageSummary
import com.whattocook.app.domain.model.AppError
import com.whattocook.app.domain.model.AppResult
import com.whattocook.app.domain.model.Plan
import com.whattocook.app.domain.model.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AccountUiState(
    val authConfigured: Boolean = false,
    val isTursoConfigured: Boolean = false,
    val isSignedIn: Boolean = false,
    val userEmail: String? = null,
    val plan: Plan = Plan.FREE,
    val usage: UsageSummary? = null,
    val usageLoading: Boolean = true,
    val usageError: AppError? = null,
    val themeMode: ThemeMode = ThemeMode.DEFAULT,
    val busy: Boolean = false,
    val actionError: AppError? = null,
    val actionSuccess: String? = null,
)

private data class Transient(
    val usageLoading: Boolean = true,
    val usageError: AppError? = null,
    val busy: Boolean = false,
    val actionError: AppError? = null,
    val actionSuccess: String? = null,
)

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val usageRepository: UsageRepository,
    private val accountRepository: AccountRepository,
    private val settings: SettingsRepository,
    private val auth: AuthRepository,
) : ViewModel() {

    private val transient = MutableStateFlow(Transient())

    val uiState: StateFlow<AccountUiState> = combine(
        usageRepository.usage,
        settings.themeMode,
        auth.userId,
        auth.userEmail,
        transient,
    ) { usage, theme, userId, userEmail, t ->
        AccountUiState(
            authConfigured = auth.isConfigured,
            isTursoConfigured = auth.isTursoConfigured,
            isSignedIn = userEmail != null,
            userEmail = userEmail,
            plan = usage?.plan ?: Plan.FREE,
            usage = usage?.let { UsageLimit.summarize(it, System.currentTimeMillis()) },
            usageLoading = t.usageLoading && usage == null,
            usageError = t.usageError.takeIf { usage == null },
            themeMode = theme,
            busy = t.busy,
            actionError = t.actionError,
            actionSuccess = t.actionSuccess,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AccountUiState())

    init {
        refreshUsage()
    }

    fun refreshUsage() {
        viewModelScope.launch {
            transient.value = transient.value.copy(usageLoading = true, usageError = null)
            val result = usageRepository.refresh()
            transient.value = transient.value.copy(
                usageLoading = false,
                usageError = (result as? AppResult.Failure)?.error,
            )
        }
    }

    fun setTheme(mode: ThemeMode) {
        viewModelScope.launch { settings.setThemeMode(mode) }
    }

    fun signInWithEmail(email: String, pass: String, onSuccess: () -> Unit) {
        if (transient.value.busy) return
        viewModelScope.launch {
            transient.value = transient.value.copy(busy = true, actionError = null)
            val result = accountRepository.signInWithEmail(email, pass)
            when (result) {
                is AppResult.Success -> {
                    transient.value = transient.value.copy(
                        busy = false,
                        actionSuccess = "Welcome back! Signed in as $email",
                    )
                    onSuccess()
                }
                is AppResult.Failure -> {
                    transient.value = transient.value.copy(
                        busy = false,
                        actionError = result.error,
                    )
                }
            }
        }
    }

    fun signUpWithEmail(email: String, pass: String, onSuccess: () -> Unit) {
        if (transient.value.busy) return
        viewModelScope.launch {
            transient.value = transient.value.copy(busy = true, actionError = null)
            val result = accountRepository.signUpWithEmail(email, pass)
            when (result) {
                is AppResult.Success -> {
                    transient.value = transient.value.copy(
                        busy = false,
                        actionSuccess = "Account created! Signed in as $email",
                    )
                    onSuccess()
                }
                is AppResult.Failure -> {
                    transient.value = transient.value.copy(
                        busy = false,
                        actionError = result.error,
                    )
                }
            }
        }
    }

    fun signOut() = runAccountAction {
        val result = accountRepository.signOut()
        if (result is AppResult.Success) {
            transient.value = transient.value.copy(actionSuccess = "Signed out. You are now using a guest session.")
        }
        result
    }

    fun deleteAccount() = runAccountAction {
        val result = accountRepository.deleteAccount()
        if (result is AppResult.Success) {
            transient.value = transient.value.copy(actionSuccess = "Account deleted successfully.")
        }
        result
    }

    fun actionErrorShown() {
        transient.value = transient.value.copy(actionError = null)
    }

    fun actionSuccessShown() {
        transient.value = transient.value.copy(actionSuccess = null)
    }

    fun testTursoConnection(url: String, token: String, onResult: (AppResult<Unit>) -> Unit) {
        viewModelScope.launch {
            val result = accountRepository.testTursoConnection(url, token)
            onResult(result)
        }
    }

    fun saveTursoCredentials(url: String, token: String, onComplete: (AppResult<Unit>) -> Unit) {
        viewModelScope.launch {
            transient.value = transient.value.copy(busy = true, actionError = null)
            val result = accountRepository.saveTursoCredentials(url, token)
            transient.value = transient.value.copy(busy = false)
            if (result is AppResult.Success) {
                transient.value = transient.value.copy(actionSuccess = "Turso database connected successfully!")
            }
            onComplete(result)
        }
    }

    fun clearTursoCredentials() {
        viewModelScope.launch {
            accountRepository.clearTursoCredentials()
            transient.value = transient.value.copy(actionSuccess = "Turso credentials reset to defaults.")
        }
    }

    fun getEffectiveTursoUrl(onResult: (String) -> Unit) {
        viewModelScope.launch {
            onResult(accountRepository.getEffectiveDatabaseUrl())
        }
    }

    private fun runAccountAction(action: suspend () -> AppResult<Unit>) {
        if (transient.value.busy) return
        viewModelScope.launch {
            transient.value = transient.value.copy(busy = true, actionError = null)
            val result = action()
            transient.value = transient.value.copy(
                busy = false,
                actionError = (result as? AppResult.Failure)?.error,
            )
        }
    }
}
