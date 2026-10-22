package com.pixelquest.app.security

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pixelquest.app.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

/**
 * S038 on a device: with the pins in the app's network security config, the real Supabase host
 * still answers over TLS (a wrong pin would fail the handshake). Skipped for a build without a
 * hosted project, or one pointed at the local stack.
 */
@RunWith(AndroidJUnit4::class)
class PinnedConnectionInstrumentedTest {

    @Test
    fun theHostedProject_passesThePins() {
        val url = BuildConfig.SUPABASE_URL.trimEnd('/')
        assumeTrue("a hosted Supabase project", url.startsWith("https://") && url.endsWith(".supabase.co"))
        val http = OkHttpClient.Builder().connectTimeout(20, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS).build()
        val request = Request.Builder().url("$url/auth/v1/health").header("apikey", BuildConfig.SUPABASE_ANON_KEY).build()
        http.newCall(request).execute().use { response ->
            // Any HTTP answer means the TLS handshake, and so the pin check, succeeded.
            assertTrue("the server answered: ${response.code}", response.code in 200..499)
        }
    }
}
