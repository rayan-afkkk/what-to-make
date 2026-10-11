package com.whattocook.app.data.catalog

import android.content.Context
import com.whattocook.app.domain.IngredientMatcher
import com.whattocook.app.domain.model.CatalogIngredient
import com.whattocook.app.domain.model.IngredientCategory
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/** The bundled list of common ingredients in assets/ingredients.json. */
@Singleton
class IngredientCatalog @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json,
) {
    private val mutex = Mutex()
    @Volatile private var cache: Loaded? = null

    suspend fun all(): List<CatalogIngredient> = load().items

    suspend fun matcher(): IngredientMatcher = load().matcher

    /** Category for an ingredient name; unknown names fall back to [IngredientCategory.OTHER]. */
    suspend fun categoryFor(name: String): IngredientCategory {
        val loaded = load()
        val key = IngredientMatcher.normalize(name)
        return loaded.byName[name.trim().lowercase()]
            ?: loaded.byKey[key]
            ?: loaded.items.firstOrNull { loaded.matcher.matches(it.name, name) }?.category
            ?: IngredientCategory.OTHER
    }

    private suspend fun load(): Loaded {
        cache?.let { return it }
        return mutex.withLock {
            cache ?: withContext(Dispatchers.IO) { readAsset() }.also { cache = it }
        }
    }

    private fun readAsset(): Loaded {
        val items = runCatching {
            val text = context.assets.open(ASSET).bufferedReader().use { it.readText() }
            json.decodeFromString(ListSerializer(Entry.serializer()), text).map {
                CatalogIngredient(it.name, IngredientCategory.fromKey(it.category), it.aliases)
            }
        }.getOrDefault(emptyList())

        val aliases = buildMap<String, String> {
            items.forEach { item -> item.aliases.forEach { alias -> put(alias, item.name) } }
        }
        val byName = items.associate { it.name.lowercase() to it.category }
        // First entry wins, so "Peas" (produce) is not overridden by "Frozen peas".
        val byKey = mutableMapOf<String, IngredientCategory>()
        items.forEach { item ->
            byKey.putIfAbsent(IngredientMatcher.normalize(item.name), item.category)
            item.aliases.forEach { byKey.putIfAbsent(IngredientMatcher.normalize(it), item.category) }
        }
        return Loaded(items, IngredientMatcher(aliases), byName, byKey)
    }

    private class Loaded(
        val items: List<CatalogIngredient>,
        val matcher: IngredientMatcher,
        val byName: Map<String, IngredientCategory>,
        val byKey: Map<String, IngredientCategory>,
    )

    @Serializable
    private data class Entry(
        val name: String,
        val category: String,
        val aliases: List<String> = emptyList(),
    )

    private companion object {
        const val ASSET = "ingredients.json"
    }
}
