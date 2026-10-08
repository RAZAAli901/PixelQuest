package com.pixelquest.app.di

import com.pixelquest.app.BuildConfig
import com.pixelquest.app.data.remote.GEMINI_MODEL_ID
import com.pixelquest.app.data.remote.GeminiClient
import com.pixelquest.app.data.remote.GeminiClientImpl
import com.pixelquest.app.data.remote.GeminiProxyClient
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

    /**
     * Release builds (and debug builds with GEMINI_VIA_PROXY=true) go through the gemini-proxy Edge
     * Function and contain no Gemini key. Other debug builds call Gemini directly with the developer's
     * key from local.properties.
     */
    @Provides
    @Singleton
    fun provideGeminiClient(
        httpClient: HttpClient,
        aiAccess: com.pixelquest.app.domain.ai.AiAccess
    ): GeminiClient = createGeminiClient(
        viaProxy = BuildConfig.GEMINI_VIA_PROXY,
        httpClient = httpClient,
        aiAccess = aiAccess
    )

    /** Either way, nothing is sent unless an account is signed in ([AccountRequiredGeminiClient]). */
    internal fun createGeminiClient(
        viaProxy: Boolean,
        httpClient: HttpClient,
        aiAccess: com.pixelquest.app.domain.ai.AiAccess,
        supabaseUrl: String = BuildConfig.SUPABASE_URL,
        anonKey: String = BuildConfig.SUPABASE_ANON_KEY,
        geminiApiKey: String = BuildConfig.GEMINI_API_KEY
    ): GeminiClient = com.pixelquest.app.data.remote.AccountRequiredGeminiClient(
        delegate = if (viaProxy) {
            GeminiProxyClient(supabaseUrl, anonKey, { aiAccess.accessToken() }, httpClient)
        } else {
            GeminiClientImpl(apiKeyProvider = { geminiApiKey }, httpClient = httpClient, model = GEMINI_MODEL_ID)
        },
        access = aiAccess
    )

    @Provides
    @Singleton
    fun provideAiAccess(impl: com.pixelquest.app.auth.SupabaseAiAccess): com.pixelquest.app.domain.ai.AiAccess = impl

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
        aiUsageTracker: com.pixelquest.app.domain.ai.AiUsageTracker,
        @dagger.hilt.android.qualifiers.ApplicationContext context: android.content.Context
    ): com.pixelquest.app.domain.repository.HabitInsightRepository {
        return com.pixelquest.app.data.repository.HabitInsightRepositoryImpl(
            streakRepository = streakRepository,
            userProfileRepository = userProfileRepository,
            taskRepository = taskRepository,
            taskCompletionRepository = taskCompletionRepository,
            settingsRepository = settingsRepository,
            geminiClient = geminiClient,
            insightCacheRepository = insightCacheRepository,
            usageTracker = aiUsageTracker,
            onLiveInsightGenerated = {
                com.pixelquest.app.worker.InsightReadyWorker.scheduleAfter(
                    context,
                    com.pixelquest.app.data.repository.HabitInsightRepositoryImpl.MIN_CALL_INTERVAL_MS
                )
            }
        )
    }
}
