package com.whattocook.app.di

import android.content.Context
import com.whattocook.app.BuildConfig
import com.whattocook.app.data.remote.ApiService
import com.whattocook.app.data.remote.AuthApi
import com.whattocook.app.data.remote.AuthInterceptor
import com.whattocook.app.data.remote.MealDbApi
import com.whattocook.app.data.remote.TokenRefreshAuthenticator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.Cache
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        authenticator: TokenRefreshAuthenticator,
    ): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        // AI generation can take a while; keep the read timeout generous.
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .callTimeout(75, TimeUnit.SECONDS)
        .addInterceptor(authInterceptor)
        .authenticator(authenticator)
        .apply {
            if (BuildConfig.DEBUG) {
                addInterceptor(
                    HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BASIC
                        redactHeader("Authorization")
                    },
                )
            }
        }
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, json: Json): Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.BACKEND_BASE_URL)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService = retrofit.create(ApiService::class.java)

    /**
     * Client without the auth interceptor: used for sign-in calls (the interceptor would recurse)
     * and for TheMealDB (our session token must never go to a third party). Library responses
     * are cached on disk for a day so Discover scrolls smoothly and works offline once loaded.
     */
    @Provides
    @Singleton
    @Named("plain")
    fun providePlainOkHttpClient(@ApplicationContext context: Context): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .callTimeout(30, TimeUnit.SECONDS)
        .cache(Cache(File(context.cacheDir, "http"), 20L * 1024 * 1024))
        .addNetworkInterceptor { chain ->
            val response = chain.proceed(chain.request())
            if (chain.request().method == "GET" && chain.request().url.host.endsWith("themealdb.com")) {
                response.newBuilder()
                    .header("Cache-Control", "public, max-age=86400")
                    .removeHeader("Pragma")
                    .build()
            } else {
                response
            }
        }
        .build()

    @Provides
    @Singleton
    @Named("auth")
    fun provideAuthOkHttpClient(@Named("plain") client: OkHttpClient): OkHttpClient = client

    /** TheMealDB client (plain: no auth header). */
    @Provides
    @Singleton
    fun provideMealDbApi(@Named("plain") client: OkHttpClient, json: Json): MealDbApi = Retrofit.Builder()
        .baseUrl("https://www.themealdb.com/api/json/v1/${BuildConfig.MEALDB_API_KEY}/")
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(MealDbApi::class.java)

    @Provides
    @Singleton
    fun provideAuthApi(@Named("plain") client: OkHttpClient, json: Json): AuthApi = Retrofit.Builder()
        .baseUrl(BuildConfig.BACKEND_BASE_URL)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(AuthApi::class.java)
}
