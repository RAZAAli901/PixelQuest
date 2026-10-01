package com.pixelquest.app.di

import com.pixelquest.app.BuildConfig
import com.pixelquest.app.data.remote.GEMINI_MODEL_ID
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
            model = GEMINI_MODEL_ID
        )
    }

    @Provides
    @Singleton
    fun provideAiUsageTracker(
        @dagger.hilt.android.qualifiers.ApplicationContext context: android.content.Context
    ): com.pixelquest.app.domain.ai.AiUsageTracker {
        return com.pixelquest.app.data.local.prefs.PreferencesAiUsageTracker(context)
    }

    @Provides
    @Singleton
    fun provideHabitInsightRepository(
        streakRepository: com.pixelquest.app.domain.repository.StreakRepository,
        userProfileRepository: com.pixelquest.app.domain.repository.UserProfileRepository,
        taskRepository: com.pixelquest.app.domain.repository.TaskRepository,
        taskCompletionRepository: com.pixelquest.app.domain.repository.TaskCompletionRepository,
        settingsRepository: com.pixelquest.app.domain.repository.SettingsRepository,
        geminiClient: GeminiClient,
        insightCacheRepository: com.pixelquest.app.domain.repository.InsightCacheRepository,
        aiUsageTracker: com.pixelquest.app.domain.ai.AiUsageTracker
    ): com.pixelquest.app.domain.repository.HabitInsightRepository {
        return com.pixelquest.app.data.repository.HabitInsightRepositoryImpl(
            streakRepository = streakRepository,
            userProfileRepository = userProfileRepository,
            taskRepository = taskRepository,
            taskCompletionRepository = taskCompletionRepository,
            settingsRepository = settingsRepository,
            geminiClient = geminiClient,
            insightCacheRepository = insightCacheRepository,
            usageTracker = aiUsageTracker
        )
    }
}
