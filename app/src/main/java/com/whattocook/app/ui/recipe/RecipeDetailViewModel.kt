package com.whattocook.app.ui.recipe

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whattocook.app.data.catalog.IngredientCatalog
import com.whattocook.app.data.repository.PantryRepository
import com.whattocook.app.data.repository.RecipeRepository
import com.whattocook.app.data.repository.ShoppingRepository
import com.whattocook.app.domain.ServingScaler
import com.whattocook.app.domain.model.AppError
import com.whattocook.app.domain.model.Recipe
import com.whattocook.app.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class IngredientRowUi(
    val name: String,
    val amount: String,
    val inPantry: Boolean,
)

data class MissingRowUi(
    val name: String,
    val amount: String,
    val swap: String?,
)

sealed interface DetailMessage {
    data class AddedToShopping(val count: Int) : DetailMessage
}

data class RecipeDetailUiState(
    val isLoading: Boolean = true,
    val notFound: Boolean = false,
    val error: AppError? = null,
    val recipe: Recipe? = null,
    val servings: Int = 0,
    val ingredients: List<IngredientRowUi> = emptyList(),
    val missing: List<MissingRowUi> = emptyList(),
    val message: DetailMessage? = null,
)

@HiltViewModel
class RecipeDetailViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val recipeRepository: RecipeRepository,
    pantryRepository: PantryRepository,
    private val shoppingRepository: ShoppingRepository,
    private val catalog: IngredientCatalog,
) : ViewModel() {

    private val recipeId: String = checkNotNull(savedStateHandle.get<String>(Routes.ARG_ID))

    /** 0 means "use the recipe's own serving count". Survives process death. */
    private val servingsOverride = savedStateHandle.getStateFlow(KEY_SERVINGS, 0)
    private val message = MutableStateFlow<DetailMessage?>(null)

    val uiState: StateFlow<RecipeDetailUiState> = combine(
        recipeRepository.observeRecipe(recipeId),
        pantryRepository.observeItems(),
        servingsOverride,
        message,
    ) { recipe, pantry, override, msg ->
        if (recipe == null) return@combine RecipeDetailUiState(isLoading = false, notFound = true)
        val matcher = catalog.matcher()
        val pantryNames = pantry.map { it.name }
        val servings = if (override > 0) override else recipe.servings
        fun amountOf(quantity: String, unit: String) =
            listOf(ServingScaler.scale(quantity, recipe.servings, servings), unit)
                .filter { it.isNotBlank() }
                .joinToString(" ")

        val rows = recipe.ingredients.map {
            IngredientRowUi(
                name = it.name,
                amount = amountOf(it.quantity, it.unit),
                inPantry = it.inPantry || matcher.isInPantry(it.name, pantryNames),
            )
        }
        val missingNames = (rows.filterNot { it.inPantry }.map { it.name } + recipe.missing)
            .distinctBy { it.lowercase() }
            .filterNot { name -> matcher.isInPantry(name, pantryNames) || rows.any { it.inPantry && it.name.equals(name, true) } }
        val missing = missingNames.map { name ->
            MissingRowUi(
                name = name,
                amount = rows.firstOrNull { it.name.equals(name, true) }?.amount.orEmpty(),
                swap = recipe.substitutions.firstOrNull {
                    it.missing.equals(name, ignoreCase = true) || matcher.matches(it.missing, name)
                }?.swap,
            )
        }
        RecipeDetailUiState(
            isLoading = false,
            recipe = recipe,
            servings = servings,
            ingredients = rows,
            missing = missing,
            message = msg,
        )
    }
        .catch { emit(RecipeDetailUiState(isLoading = false, error = AppError.Unknown(it.message))) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecipeDetailUiState())

    fun changeServings(delta: Int) {
        val current = uiState.value.servings.takeIf { it > 0 } ?: return
        savedStateHandle[KEY_SERVINGS] = (current + delta).coerceIn(1, MAX_SERVINGS)
    }

    fun toggleSaved() {
        val recipe = uiState.value.recipe ?: return
        viewModelScope.launch { recipeRepository.setSaved(recipe.id, !recipe.isSaved) }
    }

    fun addMissingToShopping() {
        val missing = uiState.value.missing
        if (missing.isEmpty()) return
        viewModelScope.launch {
            val added = shoppingRepository.addAll(missing.map { it.name to it.amount })
            message.value = DetailMessage.AddedToShopping(added)
        }
    }

    fun messageShown() {
        message.value = null
    }

    private companion object {
        const val KEY_SERVINGS = "servings"
        const val MAX_SERVINGS = 24
    }
}
