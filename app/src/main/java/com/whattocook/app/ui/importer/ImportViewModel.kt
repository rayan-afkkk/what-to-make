package com.whattocook.app.ui.importer

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whattocook.app.data.repository.RecipeRepository
import com.whattocook.app.data.repository.UsageRepository
import com.whattocook.app.domain.RecipeText
import com.whattocook.app.domain.UsageLimit
import com.whattocook.app.domain.model.AppError
import com.whattocook.app.domain.model.AppResult
import com.whattocook.app.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface ImportStatus {
    data object Idle : ImportStatus
    data object Working : ImportStatus
    data class Failed(val error: AppError) : ImportStatus
    /** Navigation targets; the screen consumes them once. */
    data class Done(val recipeId: String) : ImportStatus
    data class LimitReached(val resetsAtMillis: Long) : ImportStatus
}

data class ImportUiState(
    val input: String = "",
    val status: ImportStatus = ImportStatus.Idle,
)

@HiltViewModel
class ImportViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val recipeRepository: RecipeRepository,
    private val usageRepository: UsageRepository,
) : ViewModel() {

    private val input = savedStateHandle.getStateFlow(KEY_INPUT, "")
    private val status = MutableStateFlow<ImportStatus>(ImportStatus.Idle)

    val uiState: StateFlow<ImportUiState> = combine(input, status) { text, status -> ImportUiState(text, status) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ImportUiState())

    init {
        // A shared link starts right away, but only once (not again after rotation).
        val shared = savedStateHandle.get<String>(Routes.ARG_URL)
        if (!shared.isNullOrBlank() && savedStateHandle.get<Boolean>(KEY_STARTED) != true) {
            savedStateHandle[KEY_INPUT] = shared
            start()
        }
    }

    fun onInputChange(value: String) {
        savedStateHandle[KEY_INPUT] = value
        if (status.value is ImportStatus.Failed) status.value = ImportStatus.Idle
    }

    fun start() {
        if (status.value == ImportStatus.Working) return
        savedStateHandle[KEY_STARTED] = true
        val url = RecipeText.extractUrl(input.value)
        if (url == null) {
            status.value = ImportStatus.Failed(AppError.Unprocessable("invalid_link"))
            return
        }
        savedStateHandle[KEY_INPUT] = url
        val usage = usageRepository.usage.value
        if (!UsageLimit.canRequest(usage, System.currentTimeMillis())) {
            status.value = ImportStatus.LimitReached(usage?.resetsAtMillis ?: 0L)
            return
        }
        status.value = ImportStatus.Working
        viewModelScope.launch {
            status.value = when (val result = recipeRepository.importFromLink(url)) {
                is AppResult.Success -> ImportStatus.Done(result.data.id)
                is AppResult.Failure -> when (val error = result.error) {
                    is AppError.LimitReached -> ImportStatus.LimitReached(error.resetsAtMillis)
                    else -> ImportStatus.Failed(error)
                }
            }
        }
    }

    /** Called after the screen navigated away for Done / LimitReached. */
    fun onNavigated() {
        status.value = ImportStatus.Idle
    }

    private companion object {
        const val KEY_INPUT = "import_input"
        const val KEY_STARTED = "import_started"
    }
}
