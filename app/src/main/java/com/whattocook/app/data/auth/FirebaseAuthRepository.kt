package com.whattocook.app.data.auth

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await

@Singleton
class FirebaseAuthRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) : AuthRepository {

    private val auth: FirebaseAuth? by lazy {
        if (FirebaseApp.getApps(context).isNotEmpty()) FirebaseAuth.getInstance() else null
    }
    private val signInMutex = Mutex()
    private val _userId = MutableStateFlow<String?>(null)

    override val isConfigured: Boolean get() = auth != null
    override val userId: StateFlow<String?> = _userId.asStateFlow()

    override suspend fun ensureSignedIn(): Boolean = signInMutex.withLock {
        val firebaseAuth = auth ?: return false
        firebaseAuth.currentUser?.let {
            _userId.value = it.uid
            return true
        }
        try {
            val user = firebaseAuth.signInAnonymously().await().user
            _userId.value = user?.uid
            user != null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Anonymous sign-in failed", e)
            false
        }
    }

    override suspend fun getIdToken(forceRefresh: Boolean): String? {
        if (!ensureSignedIn()) return null
        return try {
            auth?.currentUser?.getIdToken(forceRefresh)?.await()?.token
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Could not get ID token", e)
            null
        }
    }

    override suspend fun signOut() {
        auth?.signOut()
        _userId.value = null
    }

    override suspend fun deleteCurrentUser() {
        try {
            auth?.currentUser?.delete()?.await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Could not delete Firebase user; signing out instead", e)
        }
        signOut()
    }

    private companion object {
        const val TAG = "Auth"
    }
}
