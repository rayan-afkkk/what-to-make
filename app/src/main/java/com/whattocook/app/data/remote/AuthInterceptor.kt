package com.whattocook.app.data.remote

import com.whattocook.app.data.auth.AuthRepository
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

private const val BEARER = "Bearer "

/** Adds the session token to every request. OkHttp calls this on a background thread. */
class AuthInterceptor @Inject constructor(
    private val authRepository: AuthRepository,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking { authRepository.getToken() }
        val request = chain.request().newBuilder().apply {
            if (token != null) header("Authorization", BEARER + token)
        }.build()
        return chain.proceed(request)
    }
}

/** On a 401, swaps the rejected session for a new guest session once and retries. */
class TokenRefreshAuthenticator @Inject constructor(
    private val authRepository: AuthRepository,
) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.priorResponse != null) return null
        val rejected = response.request.header("Authorization")?.removePrefix(BEARER)
        val token = runBlocking { authRepository.refreshToken(rejected) } ?: return null
        return response.request.newBuilder()
            .header("Authorization", BEARER + token)
            .build()
    }
}
