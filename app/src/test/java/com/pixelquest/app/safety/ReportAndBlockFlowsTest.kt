package com.pixelquest.app.safety

import android.app.Application
import android.os.Looper
import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.auth.AuthUser
import com.pixelquest.app.auth.FakeAuthRepository
import com.pixelquest.app.chess.data.fake.FakeChessBackend
import com.pixelquest.app.chess.ui.IncomingChallengeViewModel
import com.pixelquest.app.darood.data.DaroodPartyRepositoryImpl
import com.pixelquest.app.data.local.AppDatabase
import com.pixelquest.app.data.remote.SupabaseResult
import com.pixelquest.app.data.remote.model.CloudProfileDto
import com.pixelquest.app.data.repository.LeaderboardRepository
import com.pixelquest.app.data.repository.LeaderboardSortMode
import com.pixelquest.app.data.repository.UserLeaderboardRank
import com.pixelquest.app.party.domain.PartyType
import com.pixelquest.app.study.chat.data.StudyChatRepositoryImpl
import com.pixelquest.app.study.chat.data.remote.StudyChatRemoteDataSource
import com.pixelquest.app.study.chat.ui.ChatNotice
import com.pixelquest.app.study.chat.ui.StudyChatViewModel
import com.pixelquest.app.study.data.StudyRepositoryImpl
import com.pixelquest.app.study.ui.StudyPartyViewModel
import com.pixelquest.app.testing.FakeChatServer
import com.pixelquest.app.testing.FakeDaroodServer
import com.pixelquest.app.testing.FakeStudyServer
import com.pixelquest.app.testing.FakeUserProfileRepository
import com.pixelquest.app.testing.TestViewModels
import com.pixelquest.app.testing.closeAfterWrites
import com.pixelquest.app.ui.screens.leaderboard.LeaderboardViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.IOException
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

/**
 * V050: Report and block flows suite.
 *
 * Covers safety mechanisms across all social and multiplayer features:
 * - Study chat: reporting messages, server refusals, network failures, blocking/unblocking sender
 * - Chess challenges: reporting abuse, duplicate report prevention, blocking challenger
 * - Leaderboard: reporting profiles, duplicate daily reports, offline recovery
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class ReportAndBlockFlowsTest {

    private val viewModels = TestViewModels()
    private val zone = ZoneId.of("UTC")
    private val server = FakeDaroodServer(me = "aria").apply {
        usernames += mapOf("aria" to "aria_k", "bram" to "bram", "bad_actor" to "troll")
        now = Instant.parse("2026-10-21T10:00:00Z")
    }
    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Application>(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        viewModels.clear()
        Dispatchers.resetMain()
        db.closeAfterWrites()
    }

    private fun waitFor(what: String, condition: () -> Boolean) = runBlocking {
        repeat(200) {
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(20))
            if (condition()) return@runBlocking
            kotlinx.coroutines.delay(20)
        }
        throw AssertionError("still waiting: $what")
    }

    @Test
    fun studyChat_report_and_block_flow_with_limits() = runBlocking {
        val party = server.seedParty("Bio Lab", "aria", "bram", "bad_actor", type = PartyType.STUDY)
        val parties = DaroodPartyRepositoryImpl(server, db.daroodDao(), { server.me }, now = { server.now.toEpochMilli() }, zone = { zone })
        parties.refreshParties()
        parties.refreshParty(party)

        var reportResult: SupabaseResult<Unit> = SupabaseResult.Success(Unit)
        var blockResult: SupabaseResult<Unit> = SupabaseResult.Success(Unit)
        val reportedMessages = mutableListOf<Pair<String, String>>()
        val serverBlocked = mutableSetOf<String>()

        val fakeChatServer = FakeChatServer(me = "aria", now = server.now).apply {
            members[party] = mutableSetOf("aria", "bram", "bad_actor")
        }
        val remoteDataSource = object : StudyChatRemoteDataSource by fakeChatServer {
            override suspend fun reportMessage(messageId: String, reason: String): SupabaseResult<Unit> {
                reportedMessages += messageId to reason
                return reportResult
            }

            override suspend fun setBlocked(userId: String, blocked: Boolean): SupabaseResult<Unit> {
                if (blockResult is SupabaseResult.Success) {
                    if (blocked) serverBlocked += userId else serverBlocked -= userId
                }
                return blockResult
            }

            override suspend fun fetchBlocked(): SupabaseResult<Set<String>> = SupabaseResult.Success(serverBlocked)
        }

        val chatRepo = StudyChatRepositoryImpl(remoteDataSource, db.chatDao(), { "aria" }, clock = { server.now.toEpochMilli() })
        val studyRepo = StudyRepositoryImpl(FakeStudyServer(server), db.studyDao(), db.partyDao(), { server.me }, clock = { server.now.toEpochMilli() }, timeZone = { zone })

        // Seed message from bad_actor
        val badMessage = fakeChatServer.seed(party, "bad_actor", "Spam / offensive link")
        chatRepo.refresh(party)

        val vm = viewModels.keep(StudyChatViewModel::class.java) {
            StudyChatViewModel(SavedStateHandle(mapOf(StudyPartyViewModel.ARG_PARTY to party)), chatRepo, parties, studyRepo)
        }
        val watching = vm.uiState.launchIn(MainScope())
        waitFor("chat loaded") { vm.uiState.value.loaded && vm.uiState.value.rows.isNotEmpty() }

        val targetRow = vm.uiState.value.rows.first { it.message.id == badMessage.id }
        assertFalse("Message is initially not blocked", targetRow.blocked)

        // 1. Report message successfully
        reportResult = SupabaseResult.Success(Unit)
        vm.report(targetRow.message, "inappropriate")
        waitFor("report sent") { vm.notices.value == ChatNotice.Reported }
        assertEquals(listOf(badMessage.id to "inappropriate"), reportedMessages)

        // 2. Report message with server refusal (e.g. rate limit / already reported)
        reportResult = SupabaseResult.ServerError(429, "too_many_reports")
        vm.report(targetRow.message, "spam")
        waitFor("refusal notice") { vm.notices.value is ChatNotice.Refused }

        // 3. Report message during network disconnect
        reportResult = SupabaseResult.NetworkError(IOException("Connection dropped"))
        vm.report(targetRow.message, "spam")
        waitFor("offline notice") { vm.notices.value == ChatNotice.Offline }

        // 4. Block user: sender's messages are masked
        blockResult = SupabaseResult.Success(Unit)
        vm.setBlocked("bad_actor", true)
        waitFor("user blocked and message masked") {
            val updated = vm.uiState.value.rows.firstOrNull { it.message.id == badMessage.id }
            updated?.blocked == true
        }
        assertTrue("User recorded in blocked set", serverBlocked.contains("bad_actor"))

        // 5. Block failure when offline sets notice
        blockResult = SupabaseResult.NetworkError(IOException("Timeout"))
        vm.setBlocked("bad_actor", false)
        waitFor("offline notice on failed block update") { vm.notices.value == ChatNotice.Offline }

        // 6. Unblock user successfully restores message visibility
        blockResult = SupabaseResult.Success(Unit)
        vm.setBlocked("bad_actor", false)
        waitFor("user unblocked and message unmasked") {
            val restored = vm.uiState.value.rows.firstOrNull { it.message.id == badMessage.id }
            restored?.blocked == false
        }
        assertFalse("User removed from blocked set", serverBlocked.contains("bad_actor"))

        watching.cancel()
    }

    @Test
    fun chessChallenge_report_and_block_flow_with_limits() = runBlocking {
        val fake = FakeChessBackend(replyDelayMs = 10)
        val parties = DaroodPartyRepositoryImpl(server, db.daroodDao(), { server.me }, now = { server.now.toEpochMilli() }, zone = { zone })

        val incomingVm = viewModels.keep(IncomingChallengeViewModel::class.java) {
            IncomingChallengeViewModel(SavedStateHandle(mapOf(IncomingChallengeViewModel.ARG to "fake-challenge-1")), fake, fake, parties)
        }
        val watching = incomingVm.state.launchIn(MainScope())
        waitFor("challenge loaded") { incomingVm.state.value.challenge != null }

        // 1. Report incoming challenge
        incomingVm.report()
        waitFor("reported") { incomingVm.state.value.reported }
        assertEquals(listOf("fake-challenge-1" to "abuse"), fake.reported)

        // 2. Duplicate report attempt is ignored
        incomingVm.report()
        assertEquals("Duplicate report ignored", 1, fake.reported.size)

        // 3. Block challenger
        incomingVm.block()
        waitFor("challenge declined on block") { incomingVm.state.value.declined }
        assertTrue("Challenger added to blocked list", fake.blocked.contains(FakeChessBackend.OPPONENT))

        watching.cancel()
    }

    @Test
    fun leaderboard_report_flow_with_limits() {
        var reportAnswer: SupabaseResult<Unit> = SupabaseResult.Success(Unit)
        val reportedProfiles = mutableListOf<Pair<String, String>>()

        val boardRepo = object : LeaderboardRepository {
            override suspend fun getProfiles(): SupabaseResult<List<CloudProfileDto>> = SupabaseResult.Success(emptyList())
            override suspend fun getTopByStreak(limit: Long, offset: Long): SupabaseResult<List<CloudProfileDto>> = SupabaseResult.Success(emptyList())
            override suspend fun getTopByLevel(limit: Long, offset: Long): SupabaseResult<List<CloudProfileDto>> = SupabaseResult.Success(emptyList())
            override suspend fun getCurrentUserRank(sortMode: LeaderboardSortMode, userId: String?): SupabaseResult<UserLeaderboardRank?> =
                SupabaseResult.Success(null)

            override suspend fun reportProfile(reportedProfileId: String, reason: String): SupabaseResult<Unit> {
                reportedProfiles += reportedProfileId to reason
                return reportAnswer
            }
        }

        val auth = object : com.pixelquest.app.auth.AuthRepository by FakeAuthRepository() {
            override val currentUser = MutableStateFlow<AuthUser?>(AuthUser("aria", "aria@pixelquest.test", null))
        }

        val vm = LeaderboardViewModel(boardRepo, auth, FakeUserProfileRepository())

        // 1. First-time report success
        reportAnswer = SupabaseResult.Success(Unit)
        vm.reportProfile("troll-user-1")
        assertEquals(
            "Report submitted. Thank you for keeping PixelQuest safe.",
            vm.uiState.value.reportMessage,
        )
        assertEquals(listOf("troll-user-1" to "Offensive display name"), reportedProfiles)

        // 2. Duplicate report on same day (409 conflict): recognizes existing report, not network error
        reportAnswer = SupabaseResult.ServerError(409, "duplicate key value violates unique constraint")
        vm.reportProfile("troll-user-1")
        assertEquals(
            LeaderboardViewModel.REPORT_ALREADY_FILED,
            vm.uiState.value.reportMessage,
        )
        assertNull(vm.uiState.value.errorMessage)

        // 3. Network error during report
        reportAnswer = SupabaseResult.NetworkError(IOException("Connection failed"))
        vm.reportProfile("troll-user-2")
        assertEquals(
            "Could not submit report: check network connection.",
            vm.uiState.value.errorMessage,
        )

        // 4. Block user from leaderboard
        vm.blockUser("troll-user-1")
        assertTrue("Blocked user in UI state", vm.uiState.value.blockedUserIds.contains("troll-user-1"))
        assertEquals("Player blocked. Their entries will no longer be visible.", vm.uiState.value.reportMessage)
    }
}
