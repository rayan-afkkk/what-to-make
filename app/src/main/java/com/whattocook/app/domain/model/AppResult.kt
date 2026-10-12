package com.whattocook.app.domain.model

sealed interface AppError {
    data object NoNetwork : AppError
    data object Timeout : AppError
    data object Unauthorized : AppError
    data class LimitReached(val resetsAtMillis: Long) : AppError
    data class Server(val code: Int) : AppError
    /** The server understood the request but couldn't do it, e.g. "no_recipe_found". */
    data class Unprocessable(val reason: String) : AppError
    data object NotConfigured : AppError
    data class Unknown(val message: String? = null) : AppError
}

sealed interface AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>
    data class Failure(val error: AppError) : AppResult<Nothing>
}

inline fun <T, R> AppResult<T>.map(transform: (T) -> R): AppResult<R> = when (this) {
    is AppResult.Success -> AppResult.Success(transform(data))
    is AppResult.Failure -> this
}
