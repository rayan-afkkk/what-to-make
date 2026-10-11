package com.whattocook.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Recipe(
    val id: String,
    val title: String,
    val summary: String,
    val timeMinutes: Int,
    val servings: Int,
    val ingredients: List<RecipeIngredient>,
    val steps: List<String>,
    val missing: List<String>,
    val substitutions: List<Substitution>,
    val createdAt: Long = 0L,
    val isSaved: Boolean = false,
    /** Photo of the dish (library recipes, and reel imports when the backend sends one). */
    val imageUrl: String? = null,
    /** Original reel, video or web page the recipe came from. */
    val sourceUrl: String? = null,
    val origin: RecipeOrigin = RecipeOrigin.AI,
    /** Short labels such as "Chicken" or "Indian". */
    val tags: List<String> = emptyList(),
)

/** Where a recipe came from. */
@Serializable
enum class RecipeOrigin {
    /** Generated from the pantry by AI. */
    AI,

    /** Broken down from a shared reel or link. */
    LINK,

    /** Opened from the built-in recipe library on Discover (free, not AI). */
    LIBRARY,
}

@Serializable
data class RecipeIngredient(
    val name: String,
    val quantity: String,
    val unit: String,
    val inPantry: Boolean,
)

@Serializable
data class Substitution(
    val missing: String,
    val swap: String,
)

/** Everything the client sends to POST /recipe. */
data class RecipeRequest(
    val ingredients: List<String>,
    val diet: String? = null,
    val timeMinutes: Int? = null,
    val equipment: List<String>? = null,
    val cuisine: String? = null,
    val allowMissing: Int = 2,
)

/** User defaults applied to every recipe request, stored in DataStore. */
data class RecipePreferences(
    val diet: String? = null,
    val maxTimeMinutes: Int? = null,
    val allowMissing: Int = 2,
    val equipment: Set<String> = emptySet(),
)
