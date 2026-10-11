package com.whattocook.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface RecipeDao {
    @Query("SELECT * FROM recipes ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<RecipeEntity>>

    @Query("SELECT * FROM recipes WHERE id = :id")
    fun observe(id: String): Flow<RecipeEntity?>

    @Query("SELECT * FROM recipes WHERE id IN (:ids)")
    fun observeByIds(ids: List<String>): Flow<List<RecipeEntity>>

    @Upsert
    suspend fun upsertAll(recipes: List<RecipeEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(recipe: RecipeEntity)

    @Query("SELECT COUNT(*) FROM recipes WHERE id = :id")
    suspend fun count(id: String): Int

    @Query("UPDATE recipes SET isSaved = :saved WHERE id = :id")
    suspend fun setSaved(id: String, saved: Boolean)
}

@Dao
interface PantryDao {
    @Query("SELECT * FROM pantry_items ORDER BY name COLLATE NOCASE")
    fun observeItems(): Flow<List<PantryItemEntity>>

    @Query("SELECT * FROM pantry_groups ORDER BY createdAt")
    fun observeGroups(): Flow<List<PantryGroupEntity>>

    @Query("SELECT name FROM pantry_items")
    suspend fun allNames(): List<String>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertItem(item: PantryItemEntity): Long

    @Query("DELETE FROM pantry_items WHERE id = :id")
    suspend fun deleteItem(id: Long)

    @Insert
    suspend fun insertGroup(group: PantryGroupEntity): Long

    @Query("UPDATE pantry_items SET groupId = NULL WHERE groupId = :groupId")
    suspend fun ungroupItems(groupId: Long)

    @Query("DELETE FROM pantry_groups WHERE id = :groupId")
    suspend fun deleteGroup(groupId: Long)
}

@Dao
interface ShoppingDao {
    @Query("SELECT * FROM shopping_items ORDER BY checked, createdAt")
    fun observeAll(): Flow<List<ShoppingItemEntity>>

    @Query("SELECT * FROM shopping_items WHERE nameKey = :nameKey AND checked = 0 LIMIT 1")
    suspend fun findOpenByKey(nameKey: String): ShoppingItemEntity?

    @Insert
    suspend fun insert(item: ShoppingItemEntity): Long

    @Query("UPDATE shopping_items SET checked = :checked WHERE id = :id")
    suspend fun setChecked(id: Long, checked: Boolean)

    @Query("DELETE FROM shopping_items WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM shopping_items WHERE checked = 1")
    suspend fun deleteChecked()
}
