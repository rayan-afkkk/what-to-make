package com.whattocook.app.data.repository

import com.whattocook.app.domain.model.AppResult
import com.whattocook.app.domain.model.Recipe
import com.whattocook.app.domain.model.RecipeRequest
import kotlinx.coroutines.flow.Flow

interface RecipeRepository {
    /** Asks the backend for recipes, stores them in the local history and returns them. */
    suspend fun generate(request: RecipeRequest): AppResult<List<Recipe>>
    fun observeAll(): Flow<List<Recipe>>
    fun observeRecipe(id: String): Flow<Recipe?>
    fun observeRecipes(ids: List<String>): Flow<List<Recipe>>
    suspend fun setSaved(id: String, saved: Boolean)
}
