package com.whattocook.app.data.auth

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.whattocook.app.BuildConfig
import com.whattocook.app.data.remote.ApiCaller
import com.whattocook.app.data.remote.AuthApi
import com.whattocook.app.data.remote.turso.TursoClient
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
 * Authentication repository supporting guest sessions as well as Turso-backed
 * email & password sign-up and sign-in.
 */
@Singleton
class GuestAuthRepository @Inject constructor(
    @AuthStore private val store: DataStore<Preferences>,
    private val authApi: AuthApi,
    private val apiCaller: ApiCaller,
    private val tursoClient: TursoClient,
) : AuthRepository {

    private val mutex = Mutex()
    private val _userId = MutableStateFlow<String?>(null)
    private val _userEmail = MutableStateFlow<String?>(null)

    override val isConfigured: Boolean = !BuildConfig.USE_FAKE_BACKEND
    override val isTursoConfigured: Boolean get() = tursoClient.isConfigured
    override val userId: StateFlow<String?> = _userId.asStateFlow()
    override val userEmail: StateFlow<String?> = _userEmail.asStateFlow()

    override suspend fun ensureSignedIn(): Boolean = getToken() != null

    override suspend fun getToken(): String? {
        mutex.withLock {
            val session = readSession()
            if (session != null) {
                _userId.value = session.id
                _userEmail.value = session.email
                return session.token
            }
            return createGuestSession()
        }
    }

    override suspend fun refreshToken(rejectedToken: String?): String? {
        mutex.withLock {
            val current = readSession()
            if (current != null && current.token != rejectedToken) return current.token
            clear()
            return createGuestSession()
        }
    }

    override suspend fun signUpWithEmail(email: String, password: String): AppResult<Unit> {
        val result = tursoClient.signUp(email, password)
        return when (result) {
            is AppResult.Failure -> result
            is AppResult.Success -> {
                val user = result.data
                val sessionToken = "turso-${UUID.randomUUID()}"
                mutex.withLock {
                    store.edit { prefs ->
                        prefs[USER_ID] = user.id
                        prefs[USER_EMAIL] = user.email
                        prefs[TOKEN] = sessionToken
                        prefs[IS_GUEST] = false
                    }
                    _userId.value = user.id
                    _userEmail.value = user.email
                }
                AppResult.Success(Unit)
            }
        }
    }

    override suspend fun signInWithEmail(email: String, password: String): AppResult<Unit> {
        val result = tursoClient.signIn(email, password)
        return when (result) {
            is AppResult.Failure -> result
            is AppResult.Success -> {
                val user = result.data
                val sessionToken = "turso-${UUID.randomUUID()}"
                mutex.withLock {
                    store.edit { prefs ->
                        prefs[USER_ID] = user.id
                        prefs[USER_EMAIL] = user.email
                        prefs[TOKEN] = sessionToken
                        prefs[IS_GUEST] = false
                    }
                    _userId.value = user.id
                    _userEmail.value = user.email
                }
                AppResult.Success(Unit)
            }
        }
    }

    override suspend fun signOut() {
        mutex.withLock {
            clear()
            createGuestSession()
        }
    }

    override suspend fun deleteCurrentUser() {
        val currentUserId = _userId.value
        val isGuest = readSession()?.isGuest ?: true
        if (!isGuest && currentUserId != null) {
            tursoClient.deleteUser(currentUserId)
        }
        mutex.withLock {
            clear()
            createGuestSession()
        }
    }

    override suspend fun testTursoConnection(url: String, token: String): AppResult<Unit> =
        tursoClient.testConnection(url, token)

    override suspend fun saveTursoCredentials(url: String, token: String): AppResult<Unit> =
        tursoClient.saveCustomCredentials(url, token)

    override suspend fun clearTursoCredentials() =
        tursoClient.clearCustomCredentials()

    override suspend fun getEffectiveDatabaseUrl(): String =
        tursoClient.getEffectiveDatabaseUrl()

    private data class StoredSession(
        val id: String,
        val token: String,
        val email: String?,
        val isGuest: Boolean,
    )

    private suspend fun readSession(): StoredSession? {
        val prefs = store.data
            .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
            .first()
        val id = prefs[USER_ID] ?: return null
        val token = prefs[TOKEN] ?: return null
        val email = prefs[USER_EMAIL]
        val isGuest = prefs[IS_GUEST] ?: (email == null)
        return StoredSession(id = id, token = token, email = email, isGuest = isGuest)
    }

    /** Must be called while holding [mutex]. */
    private suspend fun createGuestSession(): String? {
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
            it.remove(USER_EMAIL)
            it[IS_GUEST] = true
        }
        _userId.value = id
        _userEmail.value = null
        return token
    }

    private suspend fun clear() {
        store.edit { it.clear() }
        _userId.value = null
        _userEmail.value = null
    }

    private companion object {
        val USER_ID = stringPreferencesKey("user_id")
        val USER_EMAIL = stringPreferencesKey("user_email")
        val TOKEN = stringPreferencesKey("session_token")
        val IS_GUEST = booleanPreferencesKey("is_guest")
    }
}
