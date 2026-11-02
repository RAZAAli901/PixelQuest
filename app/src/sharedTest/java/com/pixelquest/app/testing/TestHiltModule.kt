package com.pixelquest.app.testing

import android.content.Context
import androidx.room.Room
import com.pixelquest.app.data.local.AppDatabase
import com.pixelquest.app.data.remote.GeminiClient
import com.pixelquest.app.data.remote.SupabaseClientWrapper
import com.pixelquest.app.di.AiModule
import com.pixelquest.app.di.AlarmModule
import com.pixelquest.app.di.CoroutineDispatchers
import com.pixelquest.app.di.DatabaseModule
import com.pixelquest.app.di.DefaultDispatcher
import com.pixelquest.app.di.DispatchersModule
import com.pixelquest.app.di.IoDispatcher
import com.pixelquest.app.di.MainDispatcher
import com.pixelquest.app.di.NetworkModule
import com.pixelquest.app.di.TimeModule
import com.pixelquest.app.domain.ai.AiAccess
import com.pixelquest.app.domain.ai.AiUsageTracker
import com.pixelquest.app.domain.repository.HabitInsightRepository
import com.pixelquest.app.scheduling.AlarmScheduler
import com.pixelquest.app.util.AppClock
import com.pixelquest.app.util.NetworkMonitor
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.ktor.client.HttpClient
import io.mockk.mockk
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.test.StandardTestDispatcher
import java.time.Clock
import java.time.Instant
import javax.inject.Singleton

/**
 * Global test Hilt module replacing real system and network dependencies with deterministic fakes (V013).
 */
@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [
        TimeModule::class,
        DispatchersModule::class,
        DatabaseModule::class,
        NetworkModule::class,
        AiModule::class,
        AlarmModule::class,
    ],
)
object TestHiltModule {

    // --- Time ---
    @Provides
    @Singleton
    fun provideFakeClock(): FakeClock = FakeClock(Instant.parse("2026-01-01T00:00:00Z"))

    @Provides
    @Singleton
    fun provideClock(fakeClock: FakeClock): Clock = fakeClock

    @Provides
    @Singleton
    fun provideAppClock(fakeClock: FakeClock): AppClock = fakeClock.asAppClock()

    // --- Coroutine Dispatchers ---
    @Provides
    @Singleton
    fun provideCoroutineDispatchers(): CoroutineDispatchers {
        val testDispatcher = StandardTestDispatcher()
        return TestDispatcherProvider.create(testDispatcher)
    }

    @Provides
    @IoDispatcher
    fun provideIoDispatcher(dispatchers: CoroutineDispatchers): CoroutineDispatcher = dispatchers.io

    @Provides
    @DefaultDispatcher
    fun provideDefaultDispatcher(dispatchers: CoroutineDispatchers): CoroutineDispatcher = dispatchers.default

    @Provides
    @MainDispatcher
    fun provideMainDispatcher(dispatchers: CoroutineDispatchers): CoroutineDispatcher = dispatchers.main

    // --- In-Memory Room Database ---
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()

    // --- Network & Supabase ---
    @Provides
    @Singleton
    fun provideFakeNetworkMonitor(): FakeNetworkMonitor = FakeNetworkMonitor(initialOnline = true)

    @Provides
    @Singleton
    fun provideNetworkMonitor(fake: FakeNetworkMonitor): NetworkMonitor = fake

    @Provides
    @Singleton
    fun provideFakeSupabaseBackend(): FakeSupabaseBackend = FakeSupabaseBackend()

    @Provides
    @Singleton
    fun provideSupabaseClientWrapper(): SupabaseClientWrapper = mockk(relaxed = true)

    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient = mockk(relaxed = true)

    @Provides
    @Singleton
    fun providePostgrest(): Postgrest = mockk(relaxed = true)

    @Provides
    @Singleton
    fun provideAuth(): Auth = mockk(relaxed = true)

    // --- Gemini AI ---
    @Provides
    @Singleton
    fun provideGeminiHttpClient(): HttpClient = mockk(relaxed = true)

    @Provides
    @Singleton
    fun provideGeminiClient(): GeminiClient = mockk(relaxed = true)

    @Provides
    @Singleton
    fun provideAiAccess(): AiAccess = mockk(relaxed = true)

    @Provides
    @Singleton
    fun provideAiUsageTracker(): AiUsageTracker = mockk(relaxed = true)

    @Provides
    @Singleton
    fun provideFakeHabitInsightRepository(): FakeHabitInsightRepository = FakeHabitInsightRepository()

    @Provides
    @Singleton
    fun provideHabitInsightRepository(fake: FakeHabitInsightRepository): HabitInsightRepository = fake

    // --- AlarmScheduler & Notifications ---
    @Provides
    @Singleton
    fun provideFakeAlarmScheduler(): FakeAlarmScheduler = FakeAlarmScheduler()

    @Provides
    @Singleton
    fun provideAlarmScheduler(fake: FakeAlarmScheduler): AlarmScheduler = fake

    @Provides
    @Singleton
    fun provideFakeNotificationManager(): FakeNotificationManager = FakeNotificationManager()
}
