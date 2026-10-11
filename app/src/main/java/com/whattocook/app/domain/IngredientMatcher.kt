package com.whattocook.app.domain

/**
 * Decides whether a pantry item covers an ingredient a recipe asks for.
 *
 * Matching works on normalized word sets so "2 large Tomatoes, chopped" matches a
 * pantry "tomato", "basmati rice" matches "rice", and aliases from the ingredient
 * catalog ("dahi" -> "yogurt") are honoured. Very generic single words such as
 * "pepper" or "oil" only match exactly, so "pepper" does not claim "bell pepper".
 *
 * Pure Kotlin so it can be unit tested on the JVM.
 */
class IngredientMatcher(aliases: Map<String, String> = emptyMap()) {

    /** normalized alias phrase -> normalized canonical phrase */
    private val aliasMap: Map<String, String> = aliases.entries.associate { (alias, canonical) ->
        normalize(alias) to normalize(canonical)
    }.filterKeys { it.isNotEmpty() }

    fun matches(pantryItem: String, recipeIngredient: String): Boolean {
        val pantry = canonicalTokens(pantryItem)
        val recipe = canonicalTokens(recipeIngredient)
        if (pantry.isEmpty() || recipe.isEmpty()) return false
        if (pantry == recipe) return true

        // A generic pantry word must match exactly ("oil" does not cover "sesame oil").
        val pantryIsGeneric = pantry.size == 1 && pantry.first() in GENERIC_WORDS
        if (!pantryIsGeneric && recipe.containsAll(pantry)) return true

        // A more specific pantry item covers a generic recipe ingredient
        // ("basmati rice" covers "rice"), unless the recipe word is generic.
        val recipeIsGeneric = recipe.size == 1 && recipe.first() in GENERIC_WORDS
        return !recipeIsGeneric && pantry.containsAll(recipe)
    }

    fun isInPantry(ingredient: String, pantry: Collection<String>): Boolean =
        pantry.any { matches(it, ingredient) }

    /** Number of [ingredients] covered by [pantry]. */
    fun countInPantry(ingredients: List<String>, pantry: Collection<String>): Int =
        ingredients.count { isInPantry(it, pantry) }

    internal fun canonicalTokens(name: String): Set<String> {
        val normalized = normalize(name)
        if (normalized.isEmpty()) return emptySet()
        val aliased = aliasMap[normalized] ?: normalized
        return aliased.split(' ').filter { it.isNotEmpty() }.toSet()
    }

    companion object {
        private val GENERIC_WORDS = setOf(
            "pepper", "oil", "sauce", "powder", "seed", "leaf", "paste",
            "milk", "butter", "cream", "flour", "stock", "vinegar", "water",
        )

        private val DESCRIPTORS = setOf(
            "fresh", "freshly", "chopped", "diced", "minced", "sliced", "large", "small",
            "medium", "finely", "roughly", "thinly", "whole", "boneless", "skinless", "ripe",
            "raw", "cooked", "dried", "dry", "optional", "grated", "shredded", "crushed",
            "peeled", "to", "taste", "of", "a", "an", "the", "and", "or", "for", "garnish",
            "cup", "tbsp", "tsp", "tablespoon", "teaspoon", "g", "kg", "gram", "ml", "l",
            "oz", "lb", "pinch", "handful", "clove", "piece", "frozen", "organic", "few",
            "some", "about", "plus", "extra", "more", "softened", "melted", "room",
            "temperature", "beaten", "halved", "cubed", "torn", "packed", "heaped", "level",
        )

        private val IRREGULAR = mapOf(
            "chilies" to "chili", "chillies" to "chili", "chilli" to "chili", "chile" to "chili",
            "chiles" to "chili", "leaves" to "leaf", "potatoes" to "potato",
            "tomatoes" to "tomato", "mangoes" to "mango", "yoghurt" to "yogurt",
            "halves" to "half", "loaves" to "loaf", "olives" to "olive", "chives" to "chive",
        )

        /** Lowercases, strips punctuation/numbers, singularizes and drops descriptor words. */
        fun normalize(name: String): String {
            val words = name.lowercase()
                .replace(Regex("[^\\p{L}\\s]"), " ")
                .split(Regex("\\s+"))
                .filter { it.isNotEmpty() }
                .map(::singularize)
            val meaningful = words.filter { it !in DESCRIPTORS }
            // "cloves" on its own is the spice, not a unit: keep the words if nothing survives.
            return (meaningful.ifEmpty { words }).joinToString(" ")
        }

        fun singularize(word: String): String {
            IRREGULAR[word]?.let { return it }
            if (word.length <= 3) return word
            return when {
                word.endsWith("ies") -> word.dropLast(3) + "y"
                word.endsWith("oes") -> word.dropLast(2)
                word.endsWith("ches") || word.endsWith("shes") ||
                    word.endsWith("sses") || word.endsWith("xes") -> word.dropLast(2)
                word.endsWith("ss") || word.endsWith("us") || word.endsWith("is") -> word
                word.endsWith("s") -> word.dropLast(1)
                else -> word
            }
        }
    }
}
