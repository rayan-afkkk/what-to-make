package com.whattocook.app.data.remote

import com.whattocook.app.data.remote.dto.LimitReachedDto
import com.whattocook.app.domain.UsageLimit
import com.whattocook.app.domain.model.AppError
import com.whattocook.app.domain.model.AppResult
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import retrofit2.Response

/** Runs a Retrofit call and maps HTTP codes and exceptions to [AppError]s the UI understands. */
@Singleton
class ApiCaller @Inject constructor(
    private val json: Json,
) {
    suspend fun <T : Any> call(block: suspend () -> Response<T>): AppResult<T> = execute {
        val response = block()
        if (response.isSuccessful) {
            val body = response.body()
            if (body != null) AppResult.Success(body) else AppResult.Failure(AppError.Unknown("Empty response"))
        } else {
            AppResult.Failure(mapHttpError(response.code(), response.errorBody()?.string()))
        }
    }

    /** For endpoints whose success response has no body (e.g. 204). */
    suspend fun callNoBody(block: suspend () -> Response<*>): AppResult<Unit> = execute {
        val response = block()
        if (response.isSuccessful) {
            AppResult.Success(Unit)
        } else {
            AppResult.Failure(mapHttpError(response.code(), response.errorBody()?.string()))
        }
    }

    private suspend fun <T> execute(block: suspend () -> AppResult<T>): AppResult<T> = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: SocketTimeoutException) {
        AppResult.Failure(AppError.Timeout)
    } catch (e: UnknownHostException) {
        AppResult.Failure(AppError.NoNetwork)
    } catch (e: ConnectException) {
        AppResult.Failure(AppError.NoNetwork)
    } catch (e: IOException) {
        AppResult.Failure(AppError.NoNetwork)
    } catch (e: SerializationException) {
        AppResult.Failure(AppError.Unknown(e.message))
    } catch (e: IllegalArgumentException) {
        AppResult.Failure(AppError.Unknown(e.message))
    }

    private fun mapHttpError(code: Int, body: String?): AppError = when (code) {
        401, 403 -> AppError.Unauthorized
        429 -> {
            val resetsAt = body?.let {
                runCatching { json.decodeFromString(LimitReachedDto.serializer(), it).resetsAt }.getOrNull()
            } ?: 0L
            AppError.LimitReached(UsageLimit.normalizeEpochMillis(resetsAt))
        }
        408, 504 -> AppError.Timeout
        else -> AppError.Server(code)
    }
}
