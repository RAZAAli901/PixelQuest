package com.pixelquest.app.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.exceptions.RestException
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

class SupabaseClientWrapperTest {

    private val mockClient: SupabaseClient = mockk(relaxed = true)
    private lateinit var wrapper: DefaultSupabaseClientWrapper

    @Before
    fun setUp() {
        wrapper = DefaultSupabaseClientWrapper(mockClient)
    }

    @Test
    fun execute_returnsSuccess_whenBlockCompletes() = runBlocking {
        val result = wrapper.execute { "success_payload" }
        assertTrue(result is SupabaseResult.Success)
        assertEquals("success_payload", (result as SupabaseResult.Success).data)
    }

    @Test
    fun execute_retriesOnTransientIoException_andSucceeds() = runBlocking {
        var attempts = 0
        val result = wrapper.execute(retries = 2, backoffBaseMs = 10L) {
            attempts++
            if (attempts == 1) {
                throw IOException("Temporary connection drop")
            }
            "recovered"
        }
        assertEquals(2, attempts)
        assertTrue(result is SupabaseResult.Success)
        assertEquals("recovered", (result as SupabaseResult.Success).data)
    }

    @Test
    fun execute_returnsNetworkError_whenRetriesExhausted() = runBlocking {
        var attempts = 0
        val result = wrapper.execute<String>(retries = 2, backoffBaseMs = 10L) {
            attempts++
            throw IOException("Persistent failure")
        }
        assertEquals(3, attempts) // initial attempt + 2 retries
        assertTrue(result is SupabaseResult.NetworkError)
    }

    @Test
    fun execute_returnsServerError_withoutRetrying_onRestException() = runBlocking {
        var attempts = 0
        val result = wrapper.execute<String>(retries = 3, backoffBaseMs = 10L) {
            attempts++
            throw RestException(error = "Row not found", description = null, statusCode = 404, message = "Row not found")
        }
        assertEquals(1, attempts) // No retry on RestException
        assertTrue(result is SupabaseResult.ServerError)
        val serverError = result as SupabaseResult.ServerError
        assertEquals(404, serverError.code)
        assertEquals("Row not found", serverError.message)
    }

    @Test
    fun execute_returnsAuthError_onAuthRelatedExceptions() = runBlocking {
        val result = wrapper.execute<String> {
            throw IllegalStateException("Invalid auth token provided")
        }
        assertTrue(result is SupabaseResult.AuthError)
    }
}
