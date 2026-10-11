package com.whattocook.app.ui.recipe

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whattocook.app.data.catalog.IngredientCatalog
import com.whattocook.app.data.repository.LibraryRepository
import com.whattocook.app.data.repository.PantryRepository
import com.whattocook.app.data.repository.RecipeRepository
import com.whattocook.app.data.repository.ShoppingRepository
import com.whattocook.app.domain.LibraryIds
import com.whattocook.app.domain.RecipeReading
import com.whattocook.app.domain.RecipeText
import com.whattocook.app.domain.ServingScaler
import com.whattocook.app.domain.model.AppResult
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

private sealed interface LibraryLoad {
    data object Idle : LibraryLoad
    data object Loading : LibraryLoad
    data class Failed(val error: AppError) : LibraryLoad
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
    /** Heads-up when an ingredient looks like pork or alcohol. */
    val mayNotBeHalal: Boolean = false,
    val haveCount: Int = 0,
    val difficulty: RecipeReading.Difficulty = RecipeReading.Difficulty.EASY,
    /** Steps the cook has ticked off. */
    val doneSteps: Set<Int> = emptySet(),
)

@HiltViewModel
class RecipeDetailViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val recipeRepository: RecipeRepository,
    pantryRepository: PantryRepository,
    private val shoppingRepository: ShoppingRepository,
    private val catalog: IngredientCatalog,
    private val libraryRepository: LibraryRepository,
) : ViewModel() {

    private val recipeId: String = checkNotNull(savedStateHandle.get<String>(Routes.ARG_ID))

    /** 0 means "use the recipe's own serving count". Survives process death. */
    private val servingsOverride = savedStateHandle.getStateFlow(KEY_SERVINGS, 0)
    private val doneSteps = savedStateHandle.getStateFlow(KEY_DONE_STEPS, IntArray(0))
    private val message = MutableStateFlow<DetailMessage?>(null)

    /** Library recipes are downloaded on first open; this tracks that download. */
    private val libraryLoad = MutableStateFlow<LibraryLoad>(LibraryLoad.Idle)

    val uiState: StateFlow<RecipeDetailUiState> = combine(
        recipeRepository.observeRecipe(recipeId),
        pantryRepository.observeItems(),
        combine(servingsOverride, doneSteps) { override, done -> override to done.toSet() },
        message,
        libraryLoad,
    ) { recipe, pantry, (override, done), msg, load ->
        if (recipe == null) {
            return@combine when (load) {
                LibraryLoad.Loading -> RecipeDetailUiState(isLoading = true)
                is LibraryLoad.Failed -> RecipeDetailUiState(isLoading = false, error = load.error)
                LibraryLoad.Idle -> RecipeDetailUiState(isLoading = false, notFound = true)
            }
        }
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
            mayNotBeHalal = RecipeText.mayNotBeHalal(recipe.ingredients.map { it.name } + recipe.title),
            haveCount = rows.count { it.inPantry },
            difficulty = RecipeReading.difficulty(recipe.steps.size, recipe.ingredients.size, recipe.timeMinutes),
            doneSteps = done,
        )
    }
        .catch { emit(RecipeDetailUiState(isLoading = false, error = AppError.Unknown(it.message))) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecipeDetailUiState())

    init {
        if (LibraryIds.mealId(recipeId) != null) loadLibraryRecipe()
    }

    fun retry() {
        if (LibraryIds.mealId(recipeId) != null) loadLibraryRecipe()
    }

    private fun loadLibraryRecipe() {
        libraryLoad.value = LibraryLoad.Loading
        viewModelScope.launch {
            libraryLoad.value = when (val result = libraryRepository.ensureStored(recipeId)) {
                is AppResult.Success -> LibraryLoad.Idle
                is AppResult.Failure -> LibraryLoad.Failed(result.error)
            }
        }
    }

    fun changeServings(delta: Int) {
        val current = uiState.value.servings.takeIf { it > 0 } ?: return
        savedStateHandle[KEY_SERVINGS] = (current + delta).coerceIn(1, MAX_SERVINGS)
    }

    /** Tick a step off (or back on) while cooking from the recipe screen. */
    fun toggleStep(index: Int) {
        val current = doneSteps.value.toMutableSet()
        if (!current.add(index)) current.remove(index)
        savedStateHandle[KEY_DONE_STEPS] = current.sorted().toIntArray()
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
        const val KEY_DONE_STEPS = "done_steps"
        const val MAX_SERVINGS = 24
    }
}
