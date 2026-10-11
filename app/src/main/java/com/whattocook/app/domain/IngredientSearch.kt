package com.whattocook.app.domain

import com.whattocook.app.domain.model.CatalogIngredient

/** Ranks catalog entries for the pantry search box. */
object IngredientSearch {

    fun search(
        catalog: List<CatalogIngredient>,
        query: String,
        exclude: Set<String>,
        limit: Int = 8,
    ): List<CatalogIngredient> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return emptyList()
        val excluded = exclude.map { it.lowercase() }.toSet()
        return catalog.asSequence()
            .filter { it.name.lowercase() !in excluded }
            .mapNotNull { item -> score(item, q)?.let { item to it } }
            .sortedWith(compareBy({ it.second }, { it.first.name.length }))
            .map { it.first }
            .take(limit)
            .toList()
    }

    /** Lower is better; null means no match. */
    private fun score(item: CatalogIngredient, q: String): Int? {
        val name = item.name.lowercase()
        return when {
            name == q -> 0
            name.startsWith(q) -> 1
            name.split(' ').any { it.startsWith(q) } -> 2
            item.aliases.any { it.lowercase().startsWith(q) } -> 3
            name.contains(q) -> 4
            item.aliases.any { it.lowercase().contains(q) } -> 5
            else -> null
        }
    }
}
