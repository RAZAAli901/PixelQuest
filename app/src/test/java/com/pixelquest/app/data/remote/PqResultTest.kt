package com.pixelquest.app.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PqResultTest {

    @Test
    fun supabaseSuccess_convertsToUnifiedSuccess() {
        val supabaseResult = SupabaseResult.Success("hello")
        assertTrue(supabaseResult.isSuccess)
        assertEquals("hello", supabaseResult.getOrNull())

        val unified = supabaseResult.toUnifiedResult()
        assertTrue(unified is PqResult.Success)
        assertEquals("hello", (unified as PqResult.Success).data)
    }

    @Test
    fun supabaseError_convertsToUnifiedFailure() {
        val error = SupabaseResult.ServerError(500, "Server failed")
        assertFalse(error.isSuccess)
        assertNull(error.getOrNull())

        val unified = error.toUnifiedResult()
        assertTrue(unified is PqResult.ServerError)
        assertEquals(500, (unified as PqResult.ServerError).code)
        assertEquals("Server failed", unified.message)
    }

    @Test
    fun geminiSuccess_convertsToUnifiedSuccess() {
        val geminiResult = GeminiResult.Success(42)
        assertTrue(geminiResult.isSuccess)
        assertEquals(42, geminiResult.getOrNull())

        val unified = geminiResult.toUnifiedResult()
        assertTrue(unified is PqResult.Success)
        assertEquals(42, (unified as PqResult.Success).data)
    }

    @Test
    fun geminiRateLimited_convertsToUnifiedRateLimited() {
        val rateLimited = GeminiResult.RateLimited(60L, "Too fast")
        assertFalse(rateLimited.isSuccess)

        val unified = rateLimited.toUnifiedResult()
        assertTrue(unified is PqResult.RateLimited)
        assertEquals(60L, (unified as PqResult.RateLimited).retryAfterSeconds)
        assertEquals("Too fast", unified.message)
    }
}
