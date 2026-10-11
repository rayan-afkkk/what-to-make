package com.whattocook.app.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.whattocook.app.domain.model.RecipePreferences
import com.whattocook.app.domain.model.ThemeMode
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** App settings stored in Preferences DataStore. */
@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    private val preferences: Flow<Preferences> = dataStore.data.catch { error ->
        if (error is IOException) emit(emptyPreferences()) else throw error
    }

    val themeMode: Flow<ThemeMode> = preferences
        .map { ThemeMode.fromKey(it[Keys.THEME]) }
        .distinctUntilChanged()

    val onboardingCompleted: Flow<Boolean> = preferences
        .map { it[Keys.ONBOARDING_DONE] ?: false }
        .distinctUntilChanged()

    val recipePreferences: Flow<RecipePreferences> = preferences.map {
        RecipePreferences(
            diet = it[Keys.DIET]?.takeIf(String::isNotBlank),
            maxTimeMinutes = it[Keys.MAX_TIME]?.takeIf { minutes -> minutes > 0 },
            allowMissing = it[Keys.ALLOW_MISSING] ?: 2,
            equipment = it[Keys.EQUIPMENT] ?: emptySet(),
        )
    }.distinctUntilChanged()

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[Keys.THEME] = mode.name }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { it[Keys.ONBOARDING_DONE] = completed }
    }

    suspend fun setRecipePreferences(prefs: RecipePreferences) {
        dataStore.edit {
            if (prefs.diet.isNullOrBlank()) it.remove(Keys.DIET) else it[Keys.DIET] = prefs.diet
            it[Keys.MAX_TIME] = prefs.maxTimeMinutes ?: 0
            it[Keys.ALLOW_MISSING] = prefs.allowMissing
            it[Keys.EQUIPMENT] = prefs.equipment
        }
    }

    /** Clears everything, including the onboarding flag (used by sign out / delete account). */
    suspend fun clearAll() {
        dataStore.edit { it.clear() }
    }

    private object Keys {
        val THEME = stringPreferencesKey("theme_mode")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_completed")
        val DIET = stringPreferencesKey("pref_diet")
        val MAX_TIME = intPreferencesKey("pref_max_time")
        val ALLOW_MISSING = intPreferencesKey("pref_allow_missing")
        val EQUIPMENT = stringSetPreferencesKey("pref_equipment")
    }
}
