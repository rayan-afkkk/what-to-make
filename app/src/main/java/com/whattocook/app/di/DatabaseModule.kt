package com.whattocook.app.di

import android.content.Context
import androidx.room.Room
import com.whattocook.app.data.local.AppDatabase
import com.whattocook.app.data.local.PantryDao
import com.whattocook.app.data.local.RecipeDao
import com.whattocook.app.data.local.ShoppingDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME).build()

    @Provides
    fun provideRecipeDao(db: AppDatabase): RecipeDao = db.recipeDao()

    @Provides
    fun providePantryDao(db: AppDatabase): PantryDao = db.pantryDao()

    @Provides
    fun provideShoppingDao(db: AppDatabase): ShoppingDao = db.shoppingDao()
}
