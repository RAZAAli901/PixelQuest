package com.pixelquest.app.testing

import com.pixelquest.app.data.remote.SupabaseResult
import java.io.IOException
import java.net.SocketTimeoutException

enum class FakeFailureType {
    NONE,
    TIMEOUT,
    NO_NETWORK,
    SERVER_ERROR,
    EMPTY_RESPONSE,
}

/**
 * Interface implemented by test fake repositories to allow deterministic injection
 * of simulated network timeouts, offline states, 500 server errors, and empty responses.
 */
interface FailureInjectable {
    var failureType: FakeFailureType

    fun setFailure(type: FakeFailureType) {
        failureType = type
    }

    fun clearFailure() {
        failureType = FakeFailureType.NONE
    }

    fun checkFailure() {
        when (failureType) {
            FakeFailureType.NONE -> {}
            FakeFailureType.TIMEOUT -> throw SocketTimeoutException("Simulated network timeout")
            FakeFailureType.NO_NETWORK -> throw IOException("Simulated no network connection")
            FakeFailureType.SERVER_ERROR -> throw RuntimeException("Simulated HTTP 500 server error")
            FakeFailureType.EMPTY_RESPONSE -> {}
        }
    }

    fun <T> checkSupabaseFailure(): SupabaseResult<T>? = when (failureType) {
        FakeFailureType.NONE -> null
        FakeFailureType.TIMEOUT -> SupabaseResult.NetworkError(
            SocketTimeoutException("Simulated network timeout"),
            "Simulated network timeout",
        )
        FakeFailureType.NO_NETWORK -> SupabaseResult.NetworkError(
            IOException("Simulated no network connection"),
            "Simulated no network connection",
        )
        FakeFailureType.SERVER_ERROR -> SupabaseResult.ServerError(500, "Simulated HTTP 500 server error")
        FakeFailureType.EMPTY_RESPONSE -> null
    }
}
