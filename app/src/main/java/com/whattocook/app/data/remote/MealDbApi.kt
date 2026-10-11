package com.whattocook.app.data.remote

import kotlinx.serialization.json.JsonObject
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * TheMealDB (https://www.themealdb.com): free recipe library with real dish photos, used for
 * the Discover browse rows. Responses are read as raw JSON because TheMealDB uses numbered
 * fields (strIngredient1..20) and returns "meals": null when nothing matches.
 */
interface MealDbApi {
    @GET("filter.php")
    suspend fun byCategory(@Query("c") category: String): Response<JsonObject>

    @GET("filter.php")
    suspend fun byArea(@Query("a") area: String): Response<JsonObject>

    @GET("search.php")
    suspend fun search(@Query("s") query: String): Response<JsonObject>

    @GET("lookup.php")
    suspend fun lookup(@Query("i") mealId: String): Response<JsonObject>

    @GET("random.php")
    suspend fun random(): Response<JsonObject>
}
