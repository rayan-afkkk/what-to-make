package com.whattocook.app.data.repository

import com.whattocook.app.data.remote.ApiCaller
import com.whattocook.app.data.remote.ApiService
import com.whattocook.app.data.remote.toDomain
import com.whattocook.app.data.remote.toDto
import com.whattocook.app.domain.UsageLimit
import com.whattocook.app.domain.model.AppError
import com.whattocook.app.domain.model.AppResult
import com.whattocook.app.domain.model.Recipe
import com.whattocook.app.domain.model.RecipeRequest
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

/** Talks to POST /recipe on your backend. */
@Singleton
class DefaultRecipeRepository @Inject constructor(
    private val api: ApiService,
    private val apiCaller: ApiCaller,
    private val store: LocalRecipeStore,
    private val usageRepository: UsageRepository,
) : RecipeRepository {

    override suspend fun generate(request: RecipeRequest): AppResult<List<Recipe>> {
        return when (val result = apiCaller.call { api.generateRecipes(request.toDto()) }) {
            is AppResult.Success -> {
                result.data.usage?.let {
                    usageRepository.onUsageReported(it.used, it.limit, UsageLimit.normalizeEpochMillis(it.resetsAt))
                }
                val now = System.currentTimeMillis()
                // Small offsets keep the server's order when sorting by date.
                val recipes = result.data.recipes.mapIndexed { index, dto -> dto.toDomain(now - index) }
                store.saveAll(recipes)
                AppResult.Success(recipes)
            }
            is AppResult.Failure -> {
                val error = result.error
                if (error is AppError.LimitReached) usageRepository.onLimitReached(error.resetsAtMillis)
                result
            }
        }
    }

    override fun observeAll(): Flow<List<Recipe>> = store.observeAll()

    override fun observeRecipe(id: String): Flow<Recipe?> = store.observe(id)

    override fun observeRecipes(ids: List<String>): Flow<List<Recipe>> = store.observeByIds(ids)

    override suspend fun setSaved(id: String, saved: Boolean) = store.setSaved(id, saved)
}
