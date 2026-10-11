package com.whattocook.app.data.remote

import com.whattocook.app.data.remote.dto.RecipeRequestDto
import com.whattocook.app.data.remote.dto.RecipeResponseDto
import com.whattocook.app.data.remote.dto.UsageResponseDto
import com.whattocook.app.data.remote.dto.VerifyPurchaseRequestDto
import com.whattocook.app.data.remote.dto.VerifyPurchaseResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * Your backend. Every call carries "Authorization: Bearer <Firebase ID token>"
 * (added by [AuthInterceptor]). The app never talks to the AI provider or the
 * database directly.
 */
interface ApiService {

    @POST("recipe")
    suspend fun generateRecipes(@Body body: RecipeRequestDto): Response<RecipeResponseDto>

    @GET("usage")
    suspend fun usage(): Response<UsageResponseDto>

    @POST("verify-purchase")
    suspend fun verifyPurchase(@Body body: VerifyPurchaseRequestDto): Response<VerifyPurchaseResponseDto>

    /** Deletes the server-side account and data for the signed-in user (required by Google Play). */
    @DELETE("account")
    suspend fun deleteAccount(): Response<Unit>
}
