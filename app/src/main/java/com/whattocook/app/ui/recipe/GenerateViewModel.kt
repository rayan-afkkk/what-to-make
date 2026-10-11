package com.whattocook.app.ui.recipe

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whattocook.app.data.repository.PantryRepository
import com.whattocook.app.data.repository.RecipeRepository
import com.whattocook.app.data.repository.UsageRepository
import com.whattocook.app.data.settings.SettingsRepository
import com.whattocook.app.domain.Cuisine
import com.whattocook.app.domain.Mood
import com.whattocook.app.domain.UsageLimit
import com.whattocook.app.domain.model.AppError
import com.whattocook.app.domain.model.AppResult
import com.whattocook.app.domain.model.Recipe
import com.whattocook.app.domain.model.RecipeRequest
import com.whattocook.app.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed interface GenerateUiState {
    data object Loading : GenerateUiState
    data object PantryEmpty : GenerateUiState
    data class Success(val recipes: List<Recipe>) : GenerateUiState
    data class Error(val error: AppError) : GenerateUiState
    data class LimitReached(val resetsAtMillis: Long) : GenerateUiState
}

@HiltViewModel
class GenerateViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val recipeRepository: RecipeRepository,
    private val pantryRepository: PantryRepository,
    private val usageRepository: UsageRepository,
    private val settings: SettingsRepository,
) : ViewModel() {

    private val mood = Mood.fromKey(savedStateHandle.get<String>(Routes.ARG_MOOD))
    private val cuisine = Cuisine.fromKey(savedStateHandle.get<String>(Routes.ARG_CUISINE))
    private val surprise = savedStateHandle.get<Boolean>(Routes.ARG_SURPRISE) ?: false
    private val usePantry = savedStateHandle.get<Boolean>(Routes.ARG_USE_PANTRY) ?: true

    private val _state = MutableStateFlow<GenerateUiState>(GenerateUiState.Loading)
    val state: StateFlow<GenerateUiState> = _state.asStateFlow()

    private var resultsJob: Job? = null

    init {
        // After rotation or process death, show the stored results instead of
        // generating (and paying for) a new batch.
        val savedIds = savedStateHandle.get<ArrayList<String>>(KEY_RESULT_IDS)
        if (savedIds != null) observeResults(savedIds) else generate()
    }

    fun retry() = generate()

    fun setSaved(id: String, saved: Boolean) {
        viewModelScope.launch { recipeRepository.setSaved(id, saved) }
    }

    private fun generate() {
        resultsJob?.cancel()
        viewModelScope.launch {
            _state.value = GenerateUiState.Loading
            val now = System.currentTimeMillis()
            val usage = usageRepository.usage.value
            if (!UsageLimit.canRequest(usage, now)) {
                _state.value = GenerateUiState.LimitReached(usage?.resetsAtMillis ?: 0L)
                return@launch
            }

            val pantry = pantryRepository.pantryNames()
            val explicitIdea = surprise || mood != null || cuisine != null
            if (usePantry && pantry.isEmpty() && !explicitIdea) {
                _state.value = GenerateUiState.PantryEmpty
                return@launch
            }

            val prefs = settings.recipePreferences.first()
            val ingredients = if (usePantry) pantry else emptyList()
            var request = RecipeRequest(
                ingredients = ingredients,
                diet = prefs.diet,
                timeMinutes = prefs.maxTimeMinutes,
                equipment = prefs.equipment.toList().ifEmpty { null },
                cuisine = cuisine?.apiValue ?: if (surprise) Cuisine.entries.random().apiValue else null,
                // With nothing to cook from, let the backend suggest freely.
                allowMissing = if (ingredients.isEmpty()) OPEN_ALLOW_MISSING else prefs.allowMissing,
            )
            if (mood != null) request = mood.applyTo(request)

            when (val result = recipeRepository.generate(request)) {
                is AppResult.Success -> {
                    val ids = result.data.map { it.id }
                    savedStateHandle[KEY_RESULT_IDS] = ArrayList(ids)
                    observeResults(ids)
                }
                is AppResult.Failure -> {
                    val error = result.error
                    _state.value = if (error is AppError.LimitReached) {
                        GenerateUiState.LimitReached(error.resetsAtMillis)
                    } else {
                        GenerateUiState.Error(error)
                    }
                }
            }
        }
    }

    private fun observeResults(ids: List<String>) {
        resultsJob?.cancel()
        if (ids.isEmpty()) {
            _state.value = GenerateUiState.Success(emptyList())
            return
        }
        resultsJob = viewModelScope.launch {
            recipeRepository.observeRecipes(ids).collect { _state.value = GenerateUiState.Success(it) }
        }
    }

    private companion object {
        const val KEY_RESULT_IDS = "result_ids"
        const val OPEN_ALLOW_MISSING = 10
    }
}
