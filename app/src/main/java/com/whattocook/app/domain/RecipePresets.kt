package com.whattocook.app.domain

import com.whattocook.app.domain.model.RecipeRequest

/**
 * Discover moods and cuisines, expressed as tweaks to a [RecipeRequest].
 * The string values are what the backend receives, not UI copy.
 */
enum class Mood(
    val diet: String? = null,
    val timeMinutes: Int? = null,
    val equipment: List<String>? = null,
    val cuisine: String? = null,
) {
    QUICK(timeMinutes = 30),
    COMFORT(cuisine = "comfort food"),
    HIGH_PROTEIN(diet = "high-protein"),
    ONE_PAN(equipment = listOf("one pan")),
    BUDGET(diet = "budget-friendly"),
    NO_OVEN(equipment = listOf("stovetop", "microwave")),
    VEGETARIAN(diet = "vegetarian"),
    HALAL(diet = "halal"),
    UNDER_15(timeMinutes = 15),
    VEGAN(diet = "vegan"),
    BREAKFAST(cuisine = "breakfast"),
    SPICY(cuisine = "spicy"),
    LIGHT(diet = "light and healthy"),
    KID_FRIENDLY(cuisine = "kid-friendly");

    fun applyTo(request: RecipeRequest): RecipeRequest = request.copy(
        diet = diet ?: request.diet,
        timeMinutes = timeMinutes ?: request.timeMinutes,
        equipment = equipment ?: request.equipment,
        cuisine = cuisine ?: request.cuisine,
    )

    companion object {
        fun fromKey(key: String?): Mood? = entries.firstOrNull { it.name == key }
    }
}

enum class Cuisine(val apiValue: String) {
    DESI("Desi (Pakistani / North Indian)"),
    ITALIAN("Italian"),
    ASIAN("East Asian"),
    MEDITERRANEAN("Mediterranean");

    companion object {
        fun fromKey(key: String?): Cuisine? = entries.firstOrNull { it.name == key }
    }
}
