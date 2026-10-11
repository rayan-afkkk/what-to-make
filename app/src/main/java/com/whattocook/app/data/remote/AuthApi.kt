package com.whattocook.app.data.remote

import com.whattocook.app.data.remote.dto.GuestSessionDto
import retrofit2.Response
import retrofit2.http.POST

/**
 * Sign-in endpoints. Served by a separate Retrofit instance WITHOUT the auth
 * interceptor, because these calls are how the app gets its token in the first place.
 */
interface AuthApi {
    /** Creates a guest user in Turso and returns its session token. */
    @POST("auth/guest")
    suspend fun createGuest(): Response<GuestSessionDto>
}
