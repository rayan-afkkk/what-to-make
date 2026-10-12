package com.whattocook.app.data.auth

import com.whattocook.app.domain.model.AppResult
import kotlinx.coroutines.flow.StateFlow

/**
 * Authentication management. Supports guest sessions as well as user accounts
 * with email and password backed by Turso Database.
 */
interface AuthRepository {
    /** False in fake-backend mode, where the session is local only. */
    val isConfigured: Boolean

    /** True if Turso database credentials are set in the Secrets panel or in-app setup. */
    val isTursoConfigured: Boolean

    val userId: StateFlow<String?>
    val userEmail: StateFlow<String?>

    /** Makes sure a session exists, creating a guest session if needed. */
    suspend fun ensureSignedIn(): Boolean

    /** The current session token, creating a guest session if there is none. */
    suspend fun getToken(): String?

    /**
     * Called after the server rejected [rejectedToken] with 401. Replaces it with a new
     * guest session, unless another request already did (so parallel 401s create one session).
     */
    suspend fun refreshToken(rejectedToken: String?): String?

    suspend fun signOut()
    suspend fun deleteCurrentUser()

    suspend fun signUpWithEmail(email: String, password: String): AppResult<Unit>
    suspend fun signInWithEmail(email: String, password: String): AppResult<Unit>

    suspend fun testTursoConnection(url: String, token: String): AppResult<Unit>
    suspend fun saveTursoCredentials(url: String, token: String): AppResult<Unit>
    suspend fun clearTursoCredentials()
    suspend fun getEffectiveDatabaseUrl(): String
}
