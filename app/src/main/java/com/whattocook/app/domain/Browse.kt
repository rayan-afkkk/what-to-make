package com.whattocook.app.domain

/** A list of library recipes: by category, by cuisine (area) or a text search. */
data class BrowseSource(
    val type: Type,
    val value: String,
) {
    enum class Type { CATEGORY, AREA, SEARCH }
}

/** A dish in the library list (photo + name). Opening it loads the full recipe. */
data class LibraryDish(
    /** The app's recipe id for this dish, e.g. "mealdb-52772". */
    val recipeId: String,
    val title: String,
    val imageUrl: String,
)

object LibraryIds {
    private const val PREFIX = "mealdb-"

    fun recipeId(mealId: String) = PREFIX + mealId

    /** The library meal id if [recipeId] is a library recipe, otherwise null. */
    fun mealId(recipeId: String): String? = recipeId.removePrefix(PREFIX).takeIf { recipeId.startsWith(PREFIX) }
}
