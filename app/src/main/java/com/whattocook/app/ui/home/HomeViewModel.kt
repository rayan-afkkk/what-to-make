package com.whattocook.app.ui.home

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whattocook.app.data.catalog.IngredientCatalog
import com.whattocook.app.data.repository.PantryRepository
import com.whattocook.app.data.repository.RecipeRepository
import com.whattocook.app.data.repository.UsageRepository
import com.whattocook.app.domain.UsageLimit
import com.whattocook.app.domain.model.AppError
import com.whattocook.app.domain.model.Plan
import com.whattocook.app.domain.model.RecipeOrigin
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

enum class HomeFilter { ALL, RECIPES, SAVED }

data class RecipeCardUi(
    val id: String,
    val title: String,
    val summary: String,
    val haveCount: Int,
    val totalCount: Int,
    val createdAt: Long,
    val isSaved: Boolean,
    val imageUrl: String?,
)

data class HomeUiState(
    val isLoading: Boolean = true,
    val error: AppError? = null,
    val filter: HomeFilter = HomeFilter.ALL,
    val recipes: List<RecipeCardUi> = emptyList(),
    val hasAnyRecipe: Boolean = false,
    val latestRecipeId: String? = null,
    /** "Get started" cards stay until the user has done each thing once. */
    val showAddIngredientsCard: Boolean = false,
    val showFirstRecipeCard: Boolean = false,
    val showSaveCard: Boolean = false,
    val isPro: Boolean = false,
    /** Recipes left this week, or null until usage is known. */
    val recipesLeft: Int? = null,
    val weeklyLimit: Int = 0,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    recipeRepository: RecipeRepository,
    pantryRepository: PantryRepository,
    catalog: IngredientCatalog,
    private val usageRepository: UsageRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val filter = savedStateHandle.getStateFlow(KEY_FILTER, HomeFilter.ALL.name)
    private val reload = MutableStateFlow(0)

    val uiState: StateFlow<HomeUiState> = reload.flatMapLatest {
        combine(
            recipeRepository.observeAll(),
            pantryRepository.observeItems(),
            filter,
            usageRepository.usage,
        ) { allRecipes, pantry, filterName, usage ->
            val summary = usage?.let { UsageLimit.summarize(it, System.currentTimeMillis()) }
            // Library dishes only appear on Home once the user saves them.
            val recipes = allRecipes.filter { it.origin != RecipeOrigin.LIBRARY || it.isSaved }
            val matcher = catalog.matcher()
            val pantryNames = pantry.map { it.name }
            val currentFilter = HomeFilter.valueOf(filterName)
            val visible = recipes.filter {
                when (currentFilter) {
                    HomeFilter.ALL -> true
                    HomeFilter.RECIPES -> !it.isSaved
                    HomeFilter.SAVED -> it.isSaved
                }
            }
            HomeUiState(
                isLoading = false,
                filter = currentFilter,
                recipes = visible.map { recipe ->
                    RecipeCardUi(
                        id = recipe.id,
                        title = recipe.title,
                        summary = recipe.summary,
                        haveCount = matcher.countInPantry(recipe.ingredients.map { it.name }, pantryNames),
                        totalCount = recipe.ingredients.size,
                        createdAt = recipe.createdAt,
                        isSaved = recipe.isSaved,
                        imageUrl = recipe.imageUrl,
                    )
                },
                hasAnyRecipe = recipes.isNotEmpty(),
                latestRecipeId = recipes.firstOrNull()?.id,
                showAddIngredientsCard = pantry.isEmpty(),
                showFirstRecipeCard = recipes.isEmpty(),
                showSaveCard = recipes.isNotEmpty() && recipes.none { it.isSaved },
                isPro = summary?.plan == Plan.PRO,
                recipesLeft = summary?.remaining,
                weeklyLimit = summary?.limit ?: 0,
            )
        }
    }
        .catch { emit(HomeUiState(isLoading = false, error = AppError.Unknown(it.message))) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    init {
        if (usageRepository.usage.value == null) viewModelScope.launch { usageRepository.refresh() }
    }

    fun setFilter(filter: HomeFilter) {
        savedStateHandle[KEY_FILTER] = filter.name
    }

    fun retry() {
        reload.value++
    }

    private companion object {
        const val KEY_FILTER = "home_filter"
    }
}
