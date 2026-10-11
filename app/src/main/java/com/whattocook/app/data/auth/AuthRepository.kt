package com.whattocook.app.data.auth

import kotlinx.coroutines.flow.StateFlow

/** Firebase Anonymous Auth. Users never create accounts; each install is a guest. */
interface AuthRepository {
    /** False until google-services.json is added (see README). */
    val isConfigured: Boolean
    val userId: StateFlow<String?>
    suspend fun ensureSignedIn(): Boolean
    suspend fun getIdToken(forceRefresh: Boolean): String?
    suspend fun signOut()
    suspend fun deleteCurrentUser()
}
