package com.whattocook.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Generated recipes (history) and saved recipes. The full recipe is stored as JSON in [payload]. */
@Entity(tableName = "recipes")
data class RecipeEntity(
    @PrimaryKey val id: String,
    val title: String,
    val summary: String,
    val createdAt: Long,
    val isSaved: Boolean,
    val payload: String,
)

@Entity(
    tableName = "pantry_items",
    indices = [Index(value = ["nameKey"], unique = true)],
)
data class PantryItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val nameKey: String,
    val category: String,
    val groupId: Long?,
    val addedAt: Long,
)

@Entity(tableName = "pantry_groups")
data class PantryGroupEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long,
)

@Entity(tableName = "shopping_items")
data class ShoppingItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val nameKey: String,
    val quantity: String,
    val section: String,
    val checked: Boolean,
    val createdAt: Long,
)
