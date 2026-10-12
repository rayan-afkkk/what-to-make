package com.whattocook.app.di

import com.whattocook.app.BuildConfig
import com.whattocook.app.data.auth.AuthRepository
import com.whattocook.app.data.auth.GuestAuthRepository
import com.whattocook.app.data.billing.BillingRepository
import com.whattocook.app.data.billing.PlayBillingRepository
import com.whattocook.app.data.repository.AccountRepository
import com.whattocook.app.data.repository.DefaultAccountRepository
import com.whattocook.app.data.repository.DefaultPantryRepository
import com.whattocook.app.data.repository.DefaultRecipeRepository
import com.whattocook.app.data.repository.DefaultShoppingRepository
import com.whattocook.app.data.repository.DefaultUsageRepository
import com.whattocook.app.data.repository.FakeRecipeRepository
import com.whattocook.app.data.repository.FakeUsageRepository
import com.whattocook.app.data.repository.LibraryRepository
import com.whattocook.app.data.repository.MealDbLibraryRepository
import com.whattocook.app.data.repository.PantryRepository
import com.whattocook.app.data.repository.RecipeRepository
import com.whattocook.app.data.repository.ShoppingRepository
import com.whattocook.app.data.repository.UsageRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Provider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: GuestAuthRepository): AuthRepository

    @Binds
    @Singleton
    abstract fun bindPantryRepository(impl: DefaultPantryRepository): PantryRepository

    @Binds
    @Singleton
    abstract fun bindShoppingRepository(impl: DefaultShoppingRepository): ShoppingRepository

    @Binds
    @Singleton
    abstract fun bindAccountRepository(impl: DefaultAccountRepository): AccountRepository

    @Binds
    @Singleton
    abstract fun bindBillingRepository(impl: PlayBillingRepository): BillingRepository

    @Binds
    @Singleton
    abstract fun bindLibraryRepository(impl: MealDbLibraryRepository): LibraryRepository

    companion object {
        /** USE_FAKE_BACKEND=true swaps in sample data so the UI runs without a server. */
        @Provides
        @Singleton
        fun provideRecipeRepository(
            fake: Provider<FakeRecipeRepository>,
            real: Provider<DefaultRecipeRepository>,
        ): RecipeRepository = if (BuildConfig.USE_FAKE_BACKEND) fake.get() else real.get()

        @Provides
        @Singleton
        fun provideUsageRepository(
            fake: Provider<FakeUsageRepository>,
            real: Provider<DefaultUsageRepository>,
        ): UsageRepository = if (BuildConfig.USE_FAKE_BACKEND) fake.get() else real.get()
    }
}
