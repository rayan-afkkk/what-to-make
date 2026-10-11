package com.whattocook.app.ui.recipe

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whattocook.app.data.repository.RecipeRepository
import com.whattocook.app.domain.StepTimer
import com.whattocook.app.domain.StepTimerParser
import com.whattocook.app.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CookUiState(
    val isLoading: Boolean = true,
    val notFound: Boolean = false,
    val title: String = "",
    val steps: List<String> = emptyList(),
    val timers: List<List<StepTimer>> = emptyList(),
    val initialStep: Int = 0,
    /** Timer key -> epoch millis when it ends. */
    val runningTimers: Map<String, Long> = emptyMap(),
    val now: Long = System.currentTimeMillis(),
) {
    fun remainingSeconds(key: String): Int? =
        runningTimers[key]?.let { end -> ((end - now + 999) / 1000).toInt().coerceAtLeast(0) }
}

fun timerKey(step: Int, index: Int) = "$step:$index"

@HiltViewModel
class CookModeViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    recipeRepository: RecipeRepository,
) : ViewModel() {

    private val recipeId: String = checkNotNull(savedStateHandle.get<String>(Routes.ARG_ID))

    /** Stored in SavedStateHandle so running timers survive rotation and process death. */
    private val timers = MutableStateFlow(
        savedStateHandle.get<HashMap<String, Long>>(KEY_TIMERS)?.toMap() ?: emptyMap(),
    )
    private val now = MutableStateFlow(System.currentTimeMillis())

    val uiState: StateFlow<CookUiState> = combine(
        recipeRepository.observeRecipe(recipeId),
        timers,
        now,
    ) { recipe, running, time ->
        if (recipe == null) {
            CookUiState(isLoading = false, notFound = true)
        } else {
            CookUiState(
                isLoading = false,
                title = recipe.title,
                steps = recipe.steps,
                timers = recipe.steps.map(StepTimerParser::parse),
                initialStep = savedStateHandle.get<Int>(KEY_STEP) ?: 0,
                runningTimers = running,
                now = time,
            )
        }
    }
        .catch { emit(CookUiState(isLoading = false, notFound = true)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CookUiState())

    init {
        viewModelScope.launch {
            while (true) {
                delay(500)
                if (timers.value.isNotEmpty()) now.value = System.currentTimeMillis()
            }
        }
    }

    fun onStepChanged(step: Int) {
        savedStateHandle[KEY_STEP] = step
    }

    /** Starts a timer; tapping a running or finished timer stops and clears it. */
    fun toggleTimer(step: Int, index: Int, seconds: Int) {
        val key = timerKey(step, index)
        val updated = timers.value.toMutableMap()
        if (key in updated) {
            updated.remove(key)
        } else {
            updated[key] = System.currentTimeMillis() + seconds * 1000L
        }
        now.value = System.currentTimeMillis()
        timers.value = updated
        savedStateHandle[KEY_TIMERS] = HashMap(updated)
    }

    private companion object {
        const val KEY_STEP = "cook_step"
        const val KEY_TIMERS = "cook_timers"
    }
}
