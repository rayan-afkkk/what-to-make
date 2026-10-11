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

/** Free users can open the first [FREE_PER_CATEGORY] dishes of each library list; the rest need Pro. */
object LibraryAccess {
    const val FREE_PER_CATEGORY = 5

    fun isLocked(index: Int, isPro: Boolean): Boolean = !isPro && index >= FREE_PER_CATEGORY

    fun lockedCount(total: Int, isPro: Boolean): Int = if (isPro) 0 else (total - FREE_PER_CATEGORY).coerceAtLeast(0)
}
