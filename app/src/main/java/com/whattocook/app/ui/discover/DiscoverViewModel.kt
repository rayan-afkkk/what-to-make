package com.whattocook.app.ui.discover

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whattocook.app.data.repository.LibraryRepository
import com.whattocook.app.data.repository.PantryRepository
import com.whattocook.app.data.repository.UsageRepository
import com.whattocook.app.domain.LibraryDish
import com.whattocook.app.domain.model.AppError
import com.whattocook.app.domain.model.AppResult
import com.whattocook.app.domain.model.Plan
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class DiscoverMode { PANTRY, ANYTHING }

sealed interface RowState {
    data object Loading : RowState
    data class Loaded(val dishes: List<LibraryDish>) : RowState
    data class Failed(val error: AppError) : RowState
}

data class DiscoverUiState(
    val isLoading: Boolean = true,
    val mode: DiscoverMode = DiscoverMode.PANTRY,
    val pantryCount: Int = 0,
    val rows: Map<String, RowState> = emptyMap(),
    /** Pro users can open every library dish; free users the first few per row. */
    val isPro: Boolean = false,
    val randomLoading: Boolean = false,
    val randomError: AppError? = null,
    /** Recipe id to open (random dish); cleared once the screen navigates. */
    val openRecipeId: String? = null,
)

private data class RandomState(
    val loading: Boolean = false,
    val error: AppError? = null,
    val openRecipeId: String? = null,
)

@HiltViewModel
class DiscoverViewModel @Inject constructor(
    pantryRepository: PantryRepository,
    private val libraryRepository: LibraryRepository,
    private val usageRepository: UsageRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val mode = savedStateHandle.getStateFlow(KEY_MODE, DiscoverMode.PANTRY.name)
    private val rows = MutableStateFlow<Map<String, RowState>>(emptyMap())
    private val random = MutableStateFlow(RandomState())

    val uiState: StateFlow<DiscoverUiState> = combine(
        mode,
        pantryRepository.observeItems(),
        rows,
        random,
        usageRepository.usage,
    ) { mode, items, rows, random, usage ->
        DiscoverUiState(
            isLoading = false,
            mode = DiscoverMode.valueOf(mode),
            pantryCount = items.size,
            rows = rows,
            isPro = usage?.plan == Plan.PRO,
            randomLoading = random.loading,
            randomError = random.error,
            openRecipeId = random.openRecipeId,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DiscoverUiState())

    init {
        // Know the plan early so Pro users never see locks.
        if (usageRepository.usage.value == null) viewModelScope.launch { usageRepository.refresh() }
    }

    fun setMode(mode: DiscoverMode) {
        savedStateHandle[KEY_MODE] = mode.name
    }

    /** Loads a library row the first time it scrolls into view (or again after a failure). */
    fun loadRow(row: LibraryRow, force: Boolean = false) {
        val current = rows.value[row.key]
        if (!force && (current is RowState.Loading || current is RowState.Loaded)) return
        rows.update { it + (row.key to RowState.Loading) }
        viewModelScope.launch {
            val state = when (val result = libraryRepository.dishes(row.source)) {
                // Keep the library's own order so the free dishes are the same everywhere.
                is AppResult.Success -> RowState.Loaded(result.data)
                is AppResult.Failure -> RowState.Failed(result.error)
            }
            rows.update { it + (row.key to state) }
        }
    }

    fun openRandomDish() {
        if (random.value.loading) return
        random.value = RandomState(loading = true)
        viewModelScope.launch {
            random.value = when (val result = libraryRepository.random()) {
                is AppResult.Success -> RandomState(openRecipeId = result.data)
                is AppResult.Failure -> RandomState(error = result.error)
            }
        }
    }

    fun onRandomHandled() {
        random.value = RandomState()
    }

    private companion object {
        const val KEY_MODE = "discover_mode"
    }
}
