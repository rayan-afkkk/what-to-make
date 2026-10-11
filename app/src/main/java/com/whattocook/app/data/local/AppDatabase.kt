package com.whattocook.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        RecipeEntity::class,
        PantryItemEntity::class,
        PantryGroupEntity::class,
        ShoppingItemEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun recipeDao(): RecipeDao
    abstract fun pantryDao(): PantryDao
    abstract fun shoppingDao(): ShoppingDao

    companion object {
        const val NAME = "what_to_cook.db"
    }
}
