package com.whattocook.app.ui.pantry

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whattocook.app.data.catalog.IngredientCatalog
import com.whattocook.app.data.repository.PantryRepository
import com.whattocook.app.data.settings.SettingsRepository
import com.whattocook.app.domain.IngredientSearch
import com.whattocook.app.domain.model.AppError
import com.whattocook.app.domain.model.CatalogIngredient
import com.whattocook.app.domain.model.IngredientCategory
import com.whattocook.app.domain.model.PantryGroup
import com.whattocook.app.domain.model.PantryItem
import com.whattocook.app.domain.model.RecipePreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface PantrySectionTitle {
    data class Group(val name: String) : PantrySectionTitle
    data class Category(val category: IngredientCategory) : PantrySectionTitle
}

data class PantrySection(
    val key: String,
    val title: PantrySectionTitle,
    val groupId: Long?,
    val items: List<PantryItem>,
)

sealed interface PantryMessage {
    data class AlreadyAdded(val name: String) : PantryMessage
    data class Added(val name: String) : PantryMessage
}

data class PantryUiState(
    val isLoading: Boolean = true,
    val error: AppError? = null,
    val sections: List<PantrySection> = emptyList(),
    val groups: List<PantryGroup> = emptyList(),
    val itemCount: Int = 0,
    val query: String = "",
    val suggestions: List<CatalogIngredient> = emptyList(),
    val canAddCustom: Boolean = false,
    val addTargetGroupId: Long? = null,
    val preferences: RecipePreferences = RecipePreferences(),
    val message: PantryMessage? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PantryViewModel @Inject constructor(
    private val pantryRepository: PantryRepository,
    private val catalog: IngredientCatalog,
    private val settings: SettingsRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val query = savedStateHandle.getStateFlow(KEY_QUERY, "")
    private val addTarget = savedStateHandle.getStateFlow(KEY_TARGET, NO_GROUP)
    private val message = MutableStateFlow<PantryMessage?>(null)
    private val reload = MutableStateFlow(0)

    private val content = reload.flatMapLatest {
        combine(
            pantryRepository.observeItems(),
            pantryRepository.observeGroups(),
            query,
        ) { items, groups, q ->
            val catalogItems = catalog.all()
            val names = items.map { it.name }.toSet()
            val suggestions = IngredientSearch.search(catalogItems, q, names)
            val trimmed = q.trim()
            Content(
                items = items,
                groups = groups,
                suggestions = suggestions,
                canAddCustom = trimmed.isNotEmpty() &&
                    suggestions.none { it.name.equals(trimmed, ignoreCase = true) } &&
                    names.none { it.equals(trimmed, ignoreCase = true) },
            )
        }
    }

    val uiState: StateFlow<PantryUiState> = combine(
        content,
        query,
        addTarget,
        settings.recipePreferences,
        message,
    ) { content, q, target, prefs, msg ->
        val groupIds = content.groups.map { it.id }.toSet()
        PantryUiState(
            isLoading = false,
            sections = buildSections(content.items, content.groups),
            groups = content.groups,
            itemCount = content.items.size,
            query = q,
            suggestions = content.suggestions,
            canAddCustom = content.canAddCustom,
            addTargetGroupId = target.takeIf { it in groupIds },
            preferences = prefs,
            message = msg,
        )
    }
        .catch { emit(PantryUiState(isLoading = false, error = AppError.Unknown(it.message))) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PantryUiState())

    fun onQueryChange(value: String) {
        savedStateHandle[KEY_QUERY] = value
    }

    fun addIngredient(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val target = addTarget.value.takeIf { it != NO_GROUP }
            val added = pantryRepository.addItem(trimmed, target)
            message.value = if (added) PantryMessage.Added(trimmed) else PantryMessage.AlreadyAdded(trimmed)
            savedStateHandle[KEY_QUERY] = ""
        }
    }

    fun removeItem(item: PantryItem) {
        viewModelScope.launch { pantryRepository.removeItem(item.id) }
    }

    fun createGroup(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val id = pantryRepository.createGroup(trimmed)
            savedStateHandle[KEY_TARGET] = id
        }
    }

    fun deleteGroup(id: Long) {
        viewModelScope.launch {
            pantryRepository.deleteGroup(id)
            if (addTarget.value == id) savedStateHandle[KEY_TARGET] = NO_GROUP
        }
    }

    fun setAddTarget(groupId: Long?) {
        savedStateHandle[KEY_TARGET] = groupId ?: NO_GROUP
    }

    fun updatePreferences(preferences: RecipePreferences) {
        viewModelScope.launch { settings.setRecipePreferences(preferences) }
    }

    fun messageShown() {
        message.value = null
    }

    fun retry() {
        reload.value++
    }

    private fun buildSections(items: List<PantryItem>, groups: List<PantryGroup>): List<PantrySection> {
        val groupIds = groups.map { it.id }.toSet()
        val groupSections = groups.map { group ->
            PantrySection(
                key = "group_${group.id}",
                title = PantrySectionTitle.Group(group.name),
                groupId = group.id,
                items = items.filter { it.groupId == group.id },
            )
        }
        val categorySections = items
            .filter { it.groupId == null || it.groupId !in groupIds }
            .groupBy { it.category }
            .toSortedMap(compareBy { it.ordinal })
            .map { (category, list) ->
                PantrySection(
                    key = "category_${category.key}",
                    title = PantrySectionTitle.Category(category),
                    groupId = null,
                    items = list,
                )
            }
        return groupSections + categorySections
    }

    private data class Content(
        val items: List<PantryItem>,
        val groups: List<PantryGroup>,
        val suggestions: List<CatalogIngredient>,
        val canAddCustom: Boolean,
    )

    private companion object {
        const val KEY_QUERY = "pantry_query"
        const val KEY_TARGET = "pantry_target"
        const val NO_GROUP = -1L
    }
}
