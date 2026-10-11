package com.whattocook.app.ui.library

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whattocook.app.data.repository.LibraryRepository
import com.whattocook.app.data.repository.UsageRepository
import com.whattocook.app.domain.BrowseSource
import com.whattocook.app.domain.LibraryDish
import com.whattocook.app.domain.model.AppError
import com.whattocook.app.domain.model.AppResult
import com.whattocook.app.domain.model.Plan
import com.whattocook.app.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface LibraryListUiState {
    data object Loading : LibraryListUiState
    data class Loaded(val dishes: List<LibraryDish>) : LibraryListUiState
    data class Failed(val error: AppError) : LibraryListUiState
}

@HiltViewModel
class LibraryListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val libraryRepository: LibraryRepository,
    private val usageRepository: UsageRepository,
) : ViewModel() {

    val isPro: StateFlow<Boolean> = usageRepository.usage
        .map { it?.plan == Plan.PRO }
        .stateIn(viewModelScope, SharingStarted.Eagerly, usageRepository.usage.value?.plan == Plan.PRO)

    val title: String = savedStateHandle.get<String>(Routes.ARG_TITLE).orEmpty()
    private val source = BrowseSource(
        type = BrowseSource.Type.valueOf(checkNotNull(savedStateHandle.get<String>(Routes.ARG_TYPE))),
        value = checkNotNull(savedStateHandle.get<String>(Routes.ARG_VALUE)),
    )

    private val _state = MutableStateFlow<LibraryListUiState>(LibraryListUiState.Loading)
    val state: StateFlow<LibraryListUiState> = _state.asStateFlow()

    init {
        load()
        if (usageRepository.usage.value == null) viewModelScope.launch { usageRepository.refresh() }
    }

    fun load() {
        _state.value = LibraryListUiState.Loading
        viewModelScope.launch {
            _state.value = when (val result = libraryRepository.dishes(source)) {
                is AppResult.Success -> LibraryListUiState.Loaded(result.data)
                is AppResult.Failure -> LibraryListUiState.Failed(result.error)
            }
        }
    }
}
