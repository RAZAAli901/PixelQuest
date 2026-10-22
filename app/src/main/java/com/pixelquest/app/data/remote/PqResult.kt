package com.pixelquest.app.data.remote

/**
 * Unified result type for PixelQuest remote, network, and database operations,
 * merging the semantics of [SupabaseResult] and [GeminiResult].
 */
sealed interface PqResult<out T> {
    val isSuccess: Boolean
    fun getOrNull(): T?

    /**
     * Standard success container.
     */
    data class Success<out T>(val data: T) : PqResult<T> {
        override val isSuccess: Boolean get() = true
        override fun getOrNull(): T = data
    }

    /**
     * Standard failure container representing network, auth, rate limit, or server errors.
     */
    sealed interface Failure : PqResult<Nothing> {
        override val isSuccess: Boolean get() = false
        override fun getOrNull(): Nothing? = null
        val message: String
        val cause: Throwable? get() = null
    }

    data class NetworkError(
        override val cause: Throwable? = null,
        override val message: String = "Network connection unavailable.",
    ) : Failure

    data class AuthError(
        override val cause: Throwable? = null,
        override val message: String = "Authentication failed.",
    ) : Failure

    data class ServerError(
        val code: Int? = null,
        override val message: String = "Server request failed.",
    ) : Failure

    data class RateLimited(
        val retryAfterSeconds: Long? = null,
        override val message: String = "Rate limit exceeded. Please wait before retrying.",
    ) : Failure

    data class FeatureDisabled(
        override val message: String = "Feature is disabled in settings.",
    ) : Failure

    data class UnexpectedError(
        override val cause: Throwable? = null,
        override val message: String = "An unexpected error occurred.",
    ) : Failure
}

/** Converts a [SupabaseResult] into a standard [PqResult]. */
fun <T> SupabaseResult<T>.toUnifiedResult(): PqResult<T> = when (this) {
    is SupabaseResult.Success -> PqResult.Success(data)
    is SupabaseResult.NetworkError -> PqResult.NetworkError(exception, message)
    is SupabaseResult.AuthError -> PqResult.AuthError(exception, message)
    is SupabaseResult.ServerError -> PqResult.ServerError(code, message)
    is SupabaseResult.UnknownError -> PqResult.UnexpectedError(exception, message)
}

/** Converts a [GeminiResult] into a standard [PqResult]. */
fun <T> GeminiResult<T>.toUnifiedResult(): PqResult<T> = when (this) {
    is GeminiResult.Success -> PqResult.Success(data)
    is GeminiResult.NetworkError -> PqResult.NetworkError(cause, message)
    is GeminiResult.RateLimited -> PqResult.RateLimited(retryAfterSeconds, message)
    is GeminiResult.ApiError -> PqResult.ServerError(statusCode, message)
    is GeminiResult.Disabled -> PqResult.FeatureDisabled(message)
    is GeminiResult.SignInRequired -> PqResult.AuthError(null, message)
    is GeminiResult.MalformedResponse -> PqResult.UnexpectedError(cause, message)
}
