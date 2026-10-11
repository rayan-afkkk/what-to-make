package com.whattocook.app.data.repository

import com.whattocook.app.data.local.RecipeDao
import com.whattocook.app.data.local.RecipeEntity
import com.whattocook.app.domain.model.Recipe
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

/** Room-backed recipe history shared by the real and the fake recipe repositories. */
@Singleton
class LocalRecipeStore @Inject constructor(
    private val dao: RecipeDao,
    private val json: Json,
) {
    fun observeAll(): Flow<List<Recipe>> = dao.observeAll().map { list -> list.mapNotNull(::toDomain) }

    fun observe(id: String): Flow<Recipe?> = dao.observe(id).map { it?.let(::toDomain) }

    fun observeByIds(ids: List<String>): Flow<List<Recipe>> = dao.observeByIds(ids).map { list ->
        list.mapNotNull(::toDomain).sortedBy { ids.indexOf(it.id) }
    }

    suspend fun saveAll(recipes: List<Recipe>) {
        dao.upsertAll(recipes.map(::toEntity))
    }

    suspend fun setSaved(id: String, saved: Boolean) = dao.setSaved(id, saved)

    private fun toDomain(entity: RecipeEntity): Recipe? = runCatching {
        json.decodeFromString(Recipe.serializer(), entity.payload)
            .copy(id = entity.id, createdAt = entity.createdAt, isSaved = entity.isSaved)
    }.getOrNull()

    private fun toEntity(recipe: Recipe) = RecipeEntity(
        id = recipe.id,
        title = recipe.title,
        summary = recipe.summary,
        createdAt = recipe.createdAt,
        isSaved = recipe.isSaved,
        payload = json.encodeToString(Recipe.serializer(), recipe),
    )
}
