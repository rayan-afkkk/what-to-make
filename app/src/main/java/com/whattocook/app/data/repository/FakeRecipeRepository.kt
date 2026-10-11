package com.whattocook.app.data.repository

import com.whattocook.app.data.catalog.IngredientCatalog
import com.whattocook.app.domain.model.AppResult
import com.whattocook.app.domain.model.Recipe
import com.whattocook.app.domain.model.RecipeRequest
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow

/**
 * Offline stand-in for the backend, enabled with USE_FAKE_BACKEND=true.
 * Returns sample recipes after a short delay and enforces the free weekly limit.
 */
@Singleton
class FakeRecipeRepository @Inject constructor(
    private val store: LocalRecipeStore,
    private val usage: FakeUsageRepository,
    private val catalog: IngredientCatalog,
) : RecipeRepository {

    override suspend fun generate(request: RecipeRequest): AppResult<List<Recipe>> {
        delay(1_800)
        val consumed = usage.consumeOne()
        if (consumed is AppResult.Failure) return consumed

        val now = System.currentTimeMillis()
        val recipes = FakeRecipes.pick(request, catalog.matcher()).mapIndexed { index, recipe ->
            recipe.copy(id = UUID.randomUUID().toString(), createdAt = now - index)
        }
        store.saveAll(recipes)
        return AppResult.Success(recipes)
    }

    override fun observeAll(): Flow<List<Recipe>> = store.observeAll()

    override fun observeRecipe(id: String): Flow<Recipe?> = store.observe(id)

    override fun observeRecipes(ids: List<String>): Flow<List<Recipe>> = store.observeByIds(ids)

    override suspend fun setSaved(id: String, saved: Boolean) = store.setSaved(id, saved)
}
