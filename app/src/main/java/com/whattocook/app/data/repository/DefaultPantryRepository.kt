package com.whattocook.app.data.repository

import androidx.room.withTransaction
import com.whattocook.app.data.catalog.IngredientCatalog
import com.whattocook.app.data.local.AppDatabase
import com.whattocook.app.data.local.PantryDao
import com.whattocook.app.data.local.PantryGroupEntity
import com.whattocook.app.data.local.PantryItemEntity
import com.whattocook.app.domain.IngredientMatcher
import com.whattocook.app.domain.model.IngredientCategory
import com.whattocook.app.domain.model.PantryGroup
import com.whattocook.app.domain.model.PantryItem
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class DefaultPantryRepository @Inject constructor(
    private val database: AppDatabase,
    private val dao: PantryDao,
    private val catalog: IngredientCatalog,
) : PantryRepository {

    override fun observeItems(): Flow<List<PantryItem>> = dao.observeItems().map { list ->
        list.map { PantryItem(it.id, it.name, IngredientCategory.fromKey(it.category), it.groupId) }
    }

    override fun observeGroups(): Flow<List<PantryGroup>> = dao.observeGroups().map { list ->
        list.map { PantryGroup(it.id, it.name) }
    }

    override suspend fun pantryNames(): List<String> = dao.allNames()

    override suspend fun addItem(name: String, groupId: Long?): Boolean {
        val display = name.trim().replaceFirstChar { it.titlecase() }
        if (display.isEmpty()) return false
        val entity = PantryItemEntity(
            name = display,
            nameKey = nameKey(display),
            category = catalog.categoryFor(display).key,
            groupId = groupId,
            addedAt = System.currentTimeMillis(),
        )
        return dao.insertItem(entity) != -1L
    }

    override suspend fun removeItem(id: Long) = dao.deleteItem(id)

    override suspend fun createGroup(name: String): Long =
        dao.insertGroup(PantryGroupEntity(name = name.trim(), createdAt = System.currentTimeMillis()))

    override suspend fun deleteGroup(id: Long) {
        database.withTransaction {
            dao.ungroupItems(id)
            dao.deleteGroup(id)
        }
    }

    companion object {
        fun nameKey(name: String): String =
            IngredientMatcher.normalize(name).ifEmpty { name.trim().lowercase() }
    }
}
