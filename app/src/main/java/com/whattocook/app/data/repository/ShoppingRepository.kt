package com.whattocook.app.data.repository

import com.whattocook.app.data.catalog.IngredientCatalog
import com.whattocook.app.data.local.ShoppingDao
import com.whattocook.app.data.local.ShoppingItemEntity
import com.whattocook.app.domain.model.IngredientCategory
import com.whattocook.app.domain.model.ShoppingItem
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface ShoppingRepository {
    fun observeItems(): Flow<List<ShoppingItem>>
    /** Returns false if the item is already on the list (and not yet checked off). */
    suspend fun add(name: String, quantity: String = ""): Boolean
    /** Adds several items and returns how many were new. */
    suspend fun addAll(items: List<Pair<String, String>>): Int
    suspend fun setChecked(id: Long, checked: Boolean)
    suspend fun delete(id: Long)
    suspend fun clearCompleted()
}

@Singleton
class DefaultShoppingRepository @Inject constructor(
    private val dao: ShoppingDao,
    private val catalog: IngredientCatalog,
) : ShoppingRepository {

    override fun observeItems(): Flow<List<ShoppingItem>> = dao.observeAll().map { list ->
        list.map {
            ShoppingItem(it.id, it.name, it.quantity, IngredientCategory.fromKey(it.section), it.checked)
        }
    }

    override suspend fun add(name: String, quantity: String): Boolean {
        val display = name.trim().replaceFirstChar { it.titlecase() }
        if (display.isEmpty()) return false
        val key = DefaultPantryRepository.nameKey(display)
        if (dao.findOpenByKey(key) != null) return false
        dao.insert(
            ShoppingItemEntity(
                name = display,
                nameKey = key,
                quantity = quantity.trim(),
                section = catalog.categoryFor(display).key,
                checked = false,
                createdAt = System.currentTimeMillis(),
            ),
        )
        return true
    }

    override suspend fun addAll(items: List<Pair<String, String>>): Int =
        items.count { (name, quantity) -> add(name, quantity) }

    override suspend fun setChecked(id: Long, checked: Boolean) = dao.setChecked(id, checked)

    override suspend fun delete(id: Long) = dao.delete(id)

    override suspend fun clearCompleted() = dao.deleteChecked()
}
