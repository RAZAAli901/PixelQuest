package com.pixelquest.app.di

import com.pixelquest.app.data.remote.GeminiClientImpl
import com.pixelquest.app.data.remote.GeminiProxyClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Release APKs are public, so they must not contain the Gemini key: release builds use the
 * gemini-proxy Edge Function, and their GEMINI_API_KEY build field is empty.
 */
class GeminiClientSelectionTest {

    private val http = HttpClient(MockEngine { respond("{}") })

    @Test
    fun viaProxy_usesTheProxyClient() {
        val client = AiModule.createGeminiClient(viaProxy = true, httpClient = http, deviceId = { "id" })
        assertTrue(client is GeminiProxyClient)
    }

    @Test
    fun otherwise_callsGeminiDirectly() {
        val client = AiModule.createGeminiClient(viaProxy = false, httpClient = http, deviceId = { "id" })
        assertTrue(client is GeminiClientImpl)
    }

    @Test
    fun releaseBuildType_hasNoGeminiKey_andUsesTheProxy() {
        val gradle = File("build.gradle.kts").takeIf { it.exists() } ?: File("app/build.gradle.kts")
        val text = gradle.readText()
        val releaseBlock = text.substringAfter("buildTypes {").substringAfter("release {").substringBefore("proguardFiles(")

        assertTrue(
            "Release builds must blank GEMINI_API_KEY",
            releaseBlock.contains("""buildConfigField("String", "GEMINI_API_KEY", "\"\"")""")
        )
        assertTrue(
            "Release builds must use the proxy",
            releaseBlock.contains("""buildConfigField("boolean", "GEMINI_VIA_PROXY", "true")""")
        )
    }
}
