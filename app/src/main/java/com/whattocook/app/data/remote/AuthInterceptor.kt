package com.whattocook.app.data.remote

import com.whattocook.app.data.auth.AuthRepository
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

/** Adds the Firebase ID token to every request. OkHttp calls this on a background thread. */
class AuthInterceptor @Inject constructor(
    private val authRepository: AuthRepository,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking { authRepository.getIdToken(forceRefresh = false) }
        val request = chain.request().newBuilder().apply {
            if (token != null) header("Authorization", "Bearer $token")
        }.build()
        return chain.proceed(request)
    }
}

/** On a 401, refreshes the ID token once and retries; gives up after that. */
class TokenRefreshAuthenticator @Inject constructor(
    private val authRepository: AuthRepository,
) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.priorResponse != null) return null
        val token = runBlocking { authRepository.getIdToken(forceRefresh = true) } ?: return null
        return response.request.newBuilder()
            .header("Authorization", "Bearer $token")
            .build()
    }
}
