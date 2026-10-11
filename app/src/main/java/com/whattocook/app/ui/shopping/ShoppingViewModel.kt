package com.whattocook.app.ui.shopping

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whattocook.app.data.repository.ShoppingRepository
import com.whattocook.app.domain.model.AppError
import com.whattocook.app.domain.model.IngredientCategory
import com.whattocook.app.domain.model.ShoppingItem
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ShoppingSection(
    val category: IngredientCategory,
    val items: List<ShoppingItem>,
)

data class ShoppingUiState(
    val isLoading: Boolean = true,
    val error: AppError? = null,
    val sections: List<ShoppingSection> = emptyList(),
    val checkedCount: Int = 0,
    val totalCount: Int = 0,
    val input: String = "",
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ShoppingViewModel @Inject constructor(
    private val repository: ShoppingRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val input = savedStateHandle.getStateFlow(KEY_INPUT, "")
    private val reload = MutableStateFlow(0)

    val uiState: StateFlow<ShoppingUiState> = combine(
        reload.flatMapLatest { repository.observeItems() }.map { items ->
            items to items.groupBy { it.section }
                .toSortedMap(compareBy { it.ordinal })
                .map { (section, list) -> ShoppingSection(section, list.sortedBy { it.checked }) }
        },
        input,
    ) { (items, sections), text ->
        ShoppingUiState(
            isLoading = false,
            sections = sections,
            checkedCount = items.count { it.checked },
            totalCount = items.size,
            input = text,
        )
    }
        .catch { emit(ShoppingUiState(isLoading = false, error = AppError.Unknown(it.message))) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ShoppingUiState())

    fun onInputChange(value: String) {
        savedStateHandle[KEY_INPUT] = value
    }

    fun addFromInput() {
        val text = input.value.trim()
        if (text.isEmpty()) return
        viewModelScope.launch {
            repository.add(text)
            savedStateHandle[KEY_INPUT] = ""
        }
    }

    fun toggle(item: ShoppingItem) {
        viewModelScope.launch { repository.setChecked(item.id, !item.checked) }
    }

    fun delete(item: ShoppingItem) {
        viewModelScope.launch { repository.delete(item.id) }
    }

    fun clearCompleted() {
        viewModelScope.launch { repository.clearCompleted() }
    }

    fun retry() {
        reload.value++
    }

    private companion object {
        const val KEY_INPUT = "shopping_input"
    }
}
