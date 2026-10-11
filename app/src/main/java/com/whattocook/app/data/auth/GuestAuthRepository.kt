package com.whattocook.app.data.auth

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.whattocook.app.BuildConfig
import com.whattocook.app.data.remote.ApiCaller
import com.whattocook.app.data.remote.AuthApi
import com.whattocook.app.di.AuthStore
import com.whattocook.app.domain.model.AppResult
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Guest sessions issued by your backend (POST /auth/guest) and stored by the backend in Turso.
 * The token is kept in its own DataStore file so clearing app settings never logs the user out
 * by accident.
 */
@Singleton
class GuestAuthRepository @Inject constructor(
    @AuthStore private val store: DataStore<Preferences>,
    private val authApi: AuthApi,
    private val apiCaller: ApiCaller,
) : AuthRepository {

    private val mutex = Mutex()
    private val _userId = MutableStateFlow<String?>(null)

    override val isConfigured: Boolean = !BuildConfig.USE_FAKE_BACKEND
    override val userId: StateFlow<String?> = _userId.asStateFlow()

    override suspend fun ensureSignedIn(): Boolean = getToken() != null

    override suspend fun getToken(): String? {
        mutex.withLock {
            val session = readSession()
            if (session != null) {
                _userId.value = session.first
                return session.second
            }
            return createSession()
        }
    }

    override suspend fun refreshToken(rejectedToken: String?): String? {
        mutex.withLock {
            val current = readSession()
            // Another request already replaced the rejected token: just use the new one.
            if (current != null && current.second != rejectedToken) return current.second
            clear()
            return createSession()
        }
    }

    override suspend fun signOut() {
        mutex.withLock { clear() }
    }

    override suspend fun deleteCurrentUser() {
        signOut()
    }

    private suspend fun readSession(): Pair<String, String>? {
        val prefs = store.data
            .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
            .first()
        val id = prefs[USER_ID] ?: return null
        val token = prefs[TOKEN] ?: return null
        return id to token
    }

    /** Must be called while holding [mutex]. */
    private suspend fun createSession(): String? {
        val (id, token) = if (BuildConfig.USE_FAKE_BACKEND) {
            "guest-${UUID.randomUUID()}" to "fake-${UUID.randomUUID()}"
        } else {
            when (val result = apiCaller.call { authApi.createGuest() }) {
                is AppResult.Success -> result.data.userId to result.data.token
                is AppResult.Failure -> return null
            }
        }
        store.edit {
            it[USER_ID] = id
            it[TOKEN] = token
        }
        _userId.value = id
        return token
    }

    private suspend fun clear() {
        store.edit { it.clear() }
        _userId.value = null
    }

    private companion object {
        val USER_ID = stringPreferencesKey("user_id")
        val TOKEN = stringPreferencesKey("session_token")
    }
}
