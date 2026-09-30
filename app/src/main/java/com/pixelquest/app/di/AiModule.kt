package com.pixelquest.app.di

import com.pixelquest.app.BuildConfig
import com.pixelquest.app.data.remote.GeminiClient
import com.pixelquest.app.data.remote.GeminiClientImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import javax.inject.Singleton

/**
 * Hilt dependency injection module providing Gemini AI generative client dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object AiModule {

    @Provides
    @Singleton
    fun provideGeminiHttpClient(): HttpClient {
        return HttpClient(Android)
    }

    @Provides
    @Singleton
    fun provideGeminiClient(
        httpClient: HttpClient
    ): GeminiClient {
        return GeminiClientImpl(
            apiKeyProvider = { BuildConfig.GEMINI_API_KEY },
            httpClient = httpClient,
            model = "gemini-1.5-flash"
        )
    }
}
