package com.whattocook.app.data.auth

import kotlinx.coroutines.flow.StateFlow

/**
 * Guest sign-in. Users never create accounts: on first launch the backend creates a
 * guest user in Turso and returns a session token, which the app sends on every request.
 */
interface AuthRepository {
    /** False in fake-backend mode, where the session is local only. */
    val isConfigured: Boolean
    val userId: StateFlow<String?>

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
}
