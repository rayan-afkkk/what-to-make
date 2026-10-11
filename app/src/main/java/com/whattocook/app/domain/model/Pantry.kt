package com.whattocook.app.domain.model

enum class IngredientCategory(val key: String) {
    PRODUCE("produce"),
    HERBS("herbs"),
    DAIRY("dairy"),
    MEAT("meat"),
    GRAINS("grains"),
    LEGUMES("legumes"),
    SPICES("spices"),
    CONDIMENTS("condiments"),
    BAKING("baking"),
    OILS("oils"),
    NUTS("nuts"),
    FROZEN("frozen"),
    OTHER("other");

    companion object {
        fun fromKey(key: String?): IngredientCategory =
            entries.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: OTHER
    }
}

data class PantryItem(
    val id: Long,
    val name: String,
    val category: IngredientCategory,
    val groupId: Long?,
)

data class PantryGroup(
    val id: Long,
    val name: String,
)

/** An entry in the bundled ingredient list (assets/ingredients.json). */
data class CatalogIngredient(
    val name: String,
    val category: IngredientCategory,
    val aliases: List<String> = emptyList(),
)

data class ShoppingItem(
    val id: Long,
    val name: String,
    val quantity: String,
    val section: IngredientCategory,
    val checked: Boolean,
)
