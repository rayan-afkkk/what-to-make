package com.whattocook.app.data.repository

import com.whattocook.app.data.remote.ApiCaller
import com.whattocook.app.data.remote.MealDbApi
import com.whattocook.app.domain.BrowseSource
import com.whattocook.app.domain.LibraryDish
import com.whattocook.app.domain.LibraryIds
import com.whattocook.app.domain.RecipeText
import com.whattocook.app.domain.model.AppError
import com.whattocook.app.domain.model.AppResult
import com.whattocook.app.domain.model.Recipe
import com.whattocook.app.domain.model.RecipeIngredient
import com.whattocook.app.domain.model.RecipeOrigin
import com.whattocook.app.domain.model.map
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/** The built-in recipe library shown on Discover. Free: it never uses the weekly AI allowance. */
interface LibraryRepository {
    suspend fun dishes(source: BrowseSource): AppResult<List<LibraryDish>>

    /** Downloads a library recipe into the local database (once) so every recipe screen can use it. */
    suspend fun ensureStored(recipeId: String): AppResult<Unit>

    /** Picks a random library recipe, stores it and returns its recipe id. */
    suspend fun random(): AppResult<String>
}

@Singleton
class MealDbLibraryRepository @Inject constructor(
    private val api: MealDbApi,
    private val apiCaller: ApiCaller,
    private val store: LocalRecipeStore,
) : LibraryRepository {

    override suspend fun dishes(source: BrowseSource): AppResult<List<LibraryDish>> {
        val query = source.value.trim()
        if (query.isEmpty()) return AppResult.Success(emptyList())
        val result = apiCaller.call {
            when (source.type) {
                BrowseSource.Type.CATEGORY -> api.byCategory(query)
                BrowseSource.Type.AREA -> api.byArea(query)
                BrowseSource.Type.SEARCH -> api.search(query)
            }
        }
        return result.map { json ->
            json.meals().mapNotNull { meal ->
                val id = meal.text("idMeal") ?: return@mapNotNull null
                val title = meal.text("strMeal") ?: return@mapNotNull null
                LibraryDish(
                    recipeId = LibraryIds.recipeId(id),
                    title = title,
                    imageUrl = meal.text("strMealThumb").orEmpty(),
                )
            }
                // Keep the library halal-friendly by default: hide obvious pork dishes.
                .filterNot { dish -> RecipeText.mayNotBeHalal(listOf(dish.title)) }
        }
    }

    override suspend fun ensureStored(recipeId: String): AppResult<Unit> {
        if (store.exists(recipeId)) return AppResult.Success(Unit)
        val mealId = LibraryIds.mealId(recipeId) ?: return AppResult.Failure(AppError.Unprocessable("not_found"))
        return when (val result = apiCaller.call { api.lookup(mealId) }) {
            is AppResult.Failure -> result
            is AppResult.Success -> {
                val meal = result.data.meals().firstOrNull()
                    ?: return AppResult.Failure(AppError.Unprocessable("not_found"))
                store.saveIfAbsent(meal.toRecipe())
                AppResult.Success(Unit)
            }
        }
    }

    override suspend fun random(): AppResult<String> {
        // Try a few times so a random pork dish is skipped rather than shown.
        repeat(4) {
            when (val result = apiCaller.call { api.random() }) {
                is AppResult.Failure -> return result
                is AppResult.Success -> {
                    val meal = result.data.meals().firstOrNull() ?: return@repeat
                    val recipe = meal.toRecipe()
                    if (!RecipeText.mayNotBeHalal(recipe.ingredients.map { it.name } + recipe.title)) {
                        store.saveIfAbsent(recipe)
                        return AppResult.Success(recipe.id)
                    }
                }
            }
        }
        return AppResult.Failure(AppError.Unprocessable("not_found"))
    }

    private fun JsonObject.meals(): List<JsonObject> =
        (this["meals"] as? JsonArray)?.mapNotNull { it as? JsonObject }.orEmpty()

    private fun JsonObject.text(key: String): String? =
        (this[key] as? JsonPrimitive)?.contentOrNull?.trim()?.takeIf { it.isNotEmpty() }

    private fun JsonObject.toRecipe(): Recipe {
        val mealId = text("idMeal").orEmpty()
        val ingredients = (1..20).mapNotNull { index ->
            val name = text("strIngredient$index") ?: return@mapNotNull null
            val (quantity, unit) = RecipeText.splitMeasure(text("strMeasure$index").orEmpty())
            RecipeIngredient(name = name.replaceFirstChar { it.titlecase() }, quantity = quantity, unit = unit, inPantry = false)
        }
        val category = text("strCategory")
        val area = text("strArea")?.takeUnless { it.equals("Unknown", ignoreCase = true) }
        return Recipe(
            id = LibraryIds.recipeId(mealId),
            title = text("strMeal").orEmpty(),
            summary = listOfNotNull(area, category?.lowercase()).joinToString(" ").replaceFirstChar { it.titlecase() },
            timeMinutes = 0,
            // TheMealDB doesn't list servings; most of its recipes serve about four.
            servings = 4,
            ingredients = ingredients,
            steps = RecipeText.splitSteps(text("strInstructions").orEmpty()),
            missing = emptyList(),
            substitutions = emptyList(),
            createdAt = System.currentTimeMillis(),
            isSaved = false,
            imageUrl = text("strMealThumb"),
            sourceUrl = text("strYoutube") ?: text("strSource"),
            origin = RecipeOrigin.LIBRARY,
            tags = listOfNotNull(category, area),
        )
    }
}
