package com.whattocook.app.data.repository

import com.whattocook.app.BuildConfig
import com.whattocook.app.data.auth.AuthRepository
import com.whattocook.app.data.local.AppDatabase
import com.whattocook.app.data.remote.ApiCaller
import com.whattocook.app.data.remote.ApiService
import com.whattocook.app.data.settings.SettingsRepository
import com.whattocook.app.domain.model.AppResult
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface AccountRepository {
    /** Ends the guest session, wipes local data and starts a fresh guest session. */
    suspend fun signOut(): AppResult<Unit>
    /** Deletes server-side data first, then the local session and all local data. */
    suspend fun deleteAccount(): AppResult<Unit>
}

@Singleton
class DefaultAccountRepository @Inject constructor(
    private val api: ApiService,
    private val apiCaller: ApiCaller,
    private val auth: AuthRepository,
    private val database: AppDatabase,
    private val settings: SettingsRepository,
    private val usageRepository: UsageRepository,
) : AccountRepository {

    override suspend fun signOut(): AppResult<Unit> {
        auth.signOut()
        clearLocalData()
        auth.ensureSignedIn()
        return AppResult.Success(Unit)
    }

    override suspend fun deleteAccount(): AppResult<Unit> {
        if (!BuildConfig.USE_FAKE_BACKEND) {
            val result = apiCaller.callNoBody { api.deleteAccount() }
            // Keep local data if the server could not delete, so the user can retry.
            if (result is AppResult.Failure) return result
        }
        auth.deleteCurrentUser()
        clearLocalData()
        auth.ensureSignedIn()
        return AppResult.Success(Unit)
    }

    private suspend fun clearLocalData() {
        withContext(Dispatchers.IO) { database.clearAllTables() }
        usageRepository.clear()
        settings.clearAll()
    }
}
