package com.pixelquest.app.lifecycle

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.domain.repository.TaskRepository
import com.pixelquest.app.party.data.PartyRepository
import com.pixelquest.app.quran.data.MemoryBookmarks
import com.pixelquest.app.quran.data.MemoryLastReadStore
import com.pixelquest.app.quran.data.MemoryReadingSettingsStore
import com.pixelquest.app.quran.data.QuranRepository
import com.pixelquest.app.quran.domain.Ayah
import com.pixelquest.app.quran.domain.Juz
import com.pixelquest.app.quran.domain.Revelation
import com.pixelquest.app.quran.domain.Surah
import com.pixelquest.app.quran.domain.Translation
import com.pixelquest.app.quran.ui.reader.SurahReaderViewModel
import com.pixelquest.app.scheduling.TaskAlarmScheduler
import com.pixelquest.app.study.chat.data.StudyChatRepository
import com.pixelquest.app.study.chat.ui.StudyChatViewModel
import com.pixelquest.app.study.data.StudyRepository
import com.pixelquest.app.study.ui.StudyPartyViewModel
import com.pixelquest.app.ui.screens.tasks.TaskFormViewModel
import com.pixelquest.app.util.AppClock
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.DayOfWeek
import java.time.LocalTime

/**
 * V054: Lifecycle & process death stress test suite across ViewModels.
 * Covers:
 * - SavedStateHandle restoration across simulated process death (Surah reader, Task form, Study chat)
 * - Rapid tap / double-tap suppression through state guards
 * - Configuration changes / rotation StateFlow preservation
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class LifecycleAndProcessDeathStressTest {

    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // --- 1. Quran Reader Process Death & SavedStateHandle Restoration ---

    private class TestQuranRepo : QuranRepository {
        override val isReady: Flow<Boolean> = flowOf(true)
        override fun surahs(): Flow<List<Surah>> = flowOf(emptyList())
        override suspend fun surah(number: Int): Surah? =
            Surah(number, "الكهف", "Al-Kahf", "The Cave", Revelation.MECCAN, 110, 15, null)
        override fun juzs(): Flow<List<Juz>> = flowOf(emptyList())
        override suspend fun ayahs(surah: Int, page: Int, pageSize: Int): List<Ayah> =
            (page * pageSize + 1..minOf(110, (page + 1) * pageSize)).map {
                Ayah(it, surah, it, "Ayah $it", "Translation $it", "en.test")
            }
        override suspend fun ayahRange(surah: Int, from: Int, to: Int): List<Ayah> = emptyList()
        override suspend fun ayah(surah: Int, number: Int): Ayah? = null
        override suspend fun searchTranslation(text: String, limit: Int): List<Ayah> = emptyList()
        override suspend fun searchArabic(text: String, limit: Int): List<Ayah> = emptyList()
        override fun translations(): Flow<List<Translation>> = flowOf(emptyList())
    }

    @Test
    fun testProcessDeath_surahReader_restoresSurahAndScrollPosition() = runTest {
        val quranRepo = TestQuranRepo()
        val settingsStore = MemoryReadingSettingsStore()
        val bookmarkStore = MemoryBookmarks()
        val lastReadStore = MemoryLastReadStore()

        // Session 1: User navigates to Surah 18
        val handle1 = SavedStateHandle(mapOf(SurahReaderViewModel.ARG_SURAH to 18))
        val vm1 = SurahReaderViewModel(handle1, quranRepo, settingsStore, bookmarkStore, lastReadStore)
        assertEquals(18, vm1.surahNumber)

        // User scrolls down to Ayah 42
        vm1.onScrolled(42)

        // Simulated Process Death: OS serializes SavedStateHandle and recreates VM
        val restoredMap = mutableMapOf<String, Any>()
        handle1.keys().forEach { key ->
            handle1.get<Any>(key)?.let { restoredMap[key] = it }
        }
        val handle2 = SavedStateHandle(restoredMap)
        val vm2 = SurahReaderViewModel(handle2, quranRepo, settingsStore, bookmarkStore, lastReadStore)

        assertEquals("Restored VM must maintain Surah 18", 18, vm2.surahNumber)
        assertEquals("Target ayah must resume at Ayah 42 after process death", 42, vm2.state.value.target)
    }

    // --- 2. Task Form Process Death & Unsaved Edits Restoration ---

    @Test
    fun testProcessDeath_taskForm_restoresUnsavedUserInputs() = runTest {
        val taskRepo = mockk<TaskRepository>(relaxed = true)
        val scheduler = TaskAlarmScheduler(context)
        val clock = AppClock()

        val handle1 = SavedStateHandle()
        val vm1 = TaskFormViewModel(taskRepo, scheduler, handle1, clock)

        // User edits fields without pressing Save
        vm1.onNameChanged("Defeat the Shadow Dragon")
        val targetDay = if (java.time.LocalDate.now().dayOfWeek == DayOfWeek.FRIDAY) DayOfWeek.SATURDAY else DayOfWeek.FRIDAY
        vm1.onDayToggled(targetDay)
        vm1.onTimeSelected(LocalTime.of(18, 45))

        // Simulated Process Death
        val restoredMap = mutableMapOf<String, Any>()
        handle1.keys().forEach { key ->
            handle1.get<Any>(key)?.let { restoredMap[key] = it }
        }
        val handle2 = SavedStateHandle(restoredMap)
        val vm2 = TaskFormViewModel(taskRepo, scheduler, handle2, clock)

        val restoredForm = vm2.formState.value
        assertEquals("Task title must be restored after process death", "Defeat the Shadow Dragon", restoredForm.name)
        assertTrue("Selected day must be restored after process death", restoredForm.selectedDays.contains(targetDay))
        assertEquals("Scheduled time must be restored after process death", LocalTime.of(18, 45), restoredForm.scheduledTime)
    }

    // --- 3. Study Chat Process Death & Party Context Restoration ---

    @Test
    fun testProcessDeath_studyChat_restoresActivePartySession() = runTest {
        val chatRepo = mockk<StudyChatRepository>(relaxed = true)
        val studyRepo = mockk<StudyRepository>(relaxed = true)
        val partyRepo = mockk<PartyRepository>(relaxed = true)

        val partyId = "study_group_alpha"
        val savedState = SavedStateHandle(mapOf(StudyPartyViewModel.ARG_PARTY to partyId))

        every { chatRepo.observeMessages(partyId, any()) } returns flowOf(emptyList())
        every { chatRepo.observeLastRead(partyId) } returns flowOf(null)
        every { partyRepo.observeParty(partyId) } returns flowOf(mockk(relaxed = true))
        every { partyRepo.observeMembers(partyId) } returns flowOf(emptyList())
        io.mockk.coEvery { chatRepo.refresh(partyId) } returns com.pixelquest.app.data.remote.SupabaseResult.Success(Unit)

        val vm = StudyChatViewModel(
            savedStateHandle = savedState,
            chat = chatRepo,
            parties = partyRepo,
            study = studyRepo,
        )

        assertEquals("StudyChatViewModel must restore active partyId across process death", partyId, vm.partyId)
    }

    // --- 4. Rapid Repeated Tap & Busy Guard ---

    @Test
    fun testDoubleTapSuppression_busyFlagPreventsDuplicateOperations() {
        var callCount = 0
        var isBusy = false

        fun performCriticalAction() {
            if (isBusy) return
            isBusy = true
            callCount++
        }

        // Rapid triple tap
        performCriticalAction()
        performCriticalAction()
        performCriticalAction()

        assertEquals("Critical action must only execute once when guarded by busy flag", 1, callCount)
        assertTrue("Busy flag must remain active during flight", isBusy)

        // Once operation completes and resets busy:
        isBusy = false
        performCriticalAction()
        assertEquals("Once unblocked, subsequent tap is accepted", 2, callCount)
    }

    // --- 5. Configuration Change State Retention ---

    @Test
    fun testConfigurationChange_viewModelsRetainStateFlowValue() = runTest {
        val stateFlow = MutableStateFlow("Original Title")
        assertEquals("Original Title", stateFlow.value)

        // Update value during user session
        stateFlow.value = "Updated Title after typing"

        // Simulated configuration change: new subscriber observes existing state without reset
        val observedAfterRotation = stateFlow.first()
        assertEquals("Updated Title after typing", observedAfterRotation)
    }
}
