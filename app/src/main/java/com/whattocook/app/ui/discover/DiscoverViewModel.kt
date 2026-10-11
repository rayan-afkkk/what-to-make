package com.whattocook.app.ui.discover

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whattocook.app.data.repository.PantryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

enum class DiscoverMode { PANTRY, ANYTHING }

data class DiscoverUiState(
    val isLoading: Boolean = true,
    val mode: DiscoverMode = DiscoverMode.PANTRY,
    val pantryCount: Int = 0,
)

@HiltViewModel
class DiscoverViewModel @Inject constructor(
    pantryRepository: PantryRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val mode = savedStateHandle.getStateFlow(KEY_MODE, DiscoverMode.PANTRY.name)

    val uiState: StateFlow<DiscoverUiState> = combine(mode, pantryRepository.observeItems()) { mode, items ->
        DiscoverUiState(isLoading = false, mode = DiscoverMode.valueOf(mode), pantryCount = items.size)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DiscoverUiState())

    fun setMode(mode: DiscoverMode) {
        savedStateHandle[KEY_MODE] = mode.name
    }

    private companion object {
        const val KEY_MODE = "discover_mode"
    }
}
