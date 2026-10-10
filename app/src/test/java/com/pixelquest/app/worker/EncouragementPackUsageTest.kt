package com.pixelquest.app.worker

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.pixelquest.app.data.local.prefs.EncouragementPackStore
import com.pixelquest.app.data.remote.GeminiApiException
import com.pixelquest.app.data.remote.GeminiClient
import com.pixelquest.app.data.remote.GeminiNetworkException
import com.pixelquest.app.domain.ai.AiUsagePolicy
import com.pixelquest.app.domain.ai.InMemoryAiUsageTracker
import com.pixelquest.app.testing.FakeSettingsRepository
import com.pixelquest.app.testing.FakeStreakRepository
import com.pixelquest.app.testing.FakeTaskCompletionRepository
import com.pixelquest.app.testing.FakeTaskRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/**
 * The daily reminder-message pack counts toward the shared AI caps only when Gemini answered, like
 * the AI Coach. It used to count every attempt before sending, so an offline try used up a call.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class EncouragementPackUsageTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val usage = InMemoryAiUsageTracker(AiUsagePolicy.MAX_CALLS_PER_DAY, AiUsagePolicy.MAX_CALLS_PER_MONTH)
    private val store = EncouragementPackStore(context.getSharedPreferences("pack_test", Context.MODE_PRIVATE))
    private val today = LocalDate.now()

    private fun run(access: com.pixelquest.app.testing.FakeAiAccess? = null, answer: () -> String) = runBlocking {
        val settings = FakeSettingsRepository(aiInsights = true).apply { aiReminderMessagesEnabled.value = true }
        val client = object : GeminiClient {
            override suspend fun generateContent(prompt: String, systemInstruction: String?): String = answer()
        }
        TestListenableWorkerBuilder<EncouragementPackWorker>(context)
            .setWorkerFactory(object : WorkerFactory() {
                override fun createWorker(appContext: Context, workerClassName: String, workerParameters: WorkerParameters) =
                    EncouragementPackWorker(
                        appContext, workerParameters, settings, client, usage, store,
                        FakeStreakRepository(), FakeTaskRepository(), FakeTaskCompletionRepository(), access
                    )
            })
            .build()
            .doWork()
    }

    @Test
    fun offline_doesNotUseUpACall() {
        run { throw GeminiNetworkException("offline", java.io.IOException("no route")) }

        assertEquals(0, usage.getDailyCallsCount(today))
        assertNull(store.load())
    }

    @Test
    fun aBuildWithoutAKey_doesNotUseUpACall() {
        run { throw GeminiApiException(401, "Gemini API key is not configured.") }

        assertEquals(0, usage.getDailyCallsCount(today))
    }

    @Test
    fun anAnswerThatCantBeUsed_stillCounts() {
        run { throw GeminiApiException(200, "Empty text in candidate") }

        assertEquals(1, usage.getDailyCallsCount(today))
        assertNull(store.load())
    }

    @Test
    fun aGoodAnswer_countsOnce_andSavesThePack() {
        run { """{"messages":["Keep the streak alive!","One quest at a time.","You showed up yesterday.","Small wins add up.","Onward, hero!"]}""" }

        assertEquals(1, usage.getDailyCallsCount(today))
        assertNotNull(store.load())
    }

    @Test
    fun signingOutWhileGeminiAnswers_savesNoLines() {
        val access = com.pixelquest.app.testing.FakeAiAccess()
        run(access) {
            access.token = null // signed out while the call was in flight
            """{"messages":["Keep the streak alive!","One quest at a time.","Small wins add up."]}"""
        }

        assertNull(store.load())
    }

    @Test
    fun signedOut_nothingIsAsked() {
        var asked = false
        run(com.pixelquest.app.testing.FakeAiAccess(token = null)) { asked = true; "{}" }

        assertEquals(false, asked)
        assertEquals(0, usage.getDailyCallsCount(today))
    }
}
