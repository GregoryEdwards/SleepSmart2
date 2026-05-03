package com.sleepsmart.app.core.result

sealed interface AppResult<out T> {
    data class Success<T>(val value: T) : AppResult<T>
    data class Failure(val error: Throwable, val message: String? = null) : AppResult<Nothing>
}

inline fun <T> appResult(block: () -> T): AppResult<T> = try {
    AppResult.Success(block())
} catch (t: Throwable) {
    AppResult.Failure(t)
}
