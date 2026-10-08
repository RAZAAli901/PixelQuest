package com.pixelquest.app.auth

import android.content.Context
import com.pixelquest.app.data.remote.SupabaseResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

/**
 * Signing in with a code sent by email: the address is checked before anything is sent, a code can
 * be asked for once a minute, the right code signs in (and links the profile like Google does), and
 * each failure says what to do.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EmailCodeSignInTest {

    private val dispatcher = StandardTestDispatcher()
    private val profiles = FakeUserProfileRepositoryForAuth()

    private class EmailAuth : AuthRepository by FakeAuthRepository() {
        val sentTo = mutableListOf<String>()
        val tried = mutableListOf<Pair<String, String>>()
        var sendResult: SupabaseResult<Unit> = SupabaseResult.Success(Unit)
        var goodCode = "123456"

        override suspend fun sendEmailCode(email: String): SupabaseResult<Unit> {
            sentTo += email
            return sendResult
        }

        override suspend fun verifyEmailCode(email: String, code: String): SupabaseResult<AuthUser> {
            tried += email to code
            return if (code == goodCode) SupabaseResult.Success(AuthUser("user-9", email, null))
            else SupabaseResult.ServerError(403, "otp_expired")
        }
    }

    private val auth = EmailAuth()
    private lateinit var viewModel: AuthViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        val context: Context = io.mockk.mockk(relaxed = true)
        viewModel = AuthViewModel(FakeGoogleAuthManager(context), auth, profiles)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun aMalformedAddress_isCaughtBeforeAnythingIsSent() = runTest(dispatcher) {
        for (bad in listOf("", "hero", "hero@", "@pixelquest.test", "hero@pixelquest", "he ro@pixelquest.test")) {
            viewModel.onEmailChanged(bad)
            viewModel.sendEmailCode()
            advanceUntilIdle()
            assertEquals(bad, EmailSignIn.INVALID_EMAIL, viewModel.emailState.value.error)
        }
        assertTrue(auth.sentTo.isEmpty())
    }

    @Test
    fun theRightCode_signsIn_andLinksTheProfile() = runTest(dispatcher) {
        viewModel.openEmailSignIn()
        viewModel.onEmailChanged("  Hero@PixelQuest.test ")
        viewModel.sendEmailCode()
        runCurrent()

        assertEquals(listOf("hero@pixelquest.test"), auth.sentTo)
        assertEquals("hero@pixelquest.test", viewModel.emailState.value.codeSentTo)

        viewModel.onCodeChanged("123 456") // pasted with a space
        viewModel.verifyEmailCode()
        runCurrent()

        assertEquals(listOf("hero@pixelquest.test" to "123456"), auth.tried)
        assertEquals(AuthUiState.SignedIn(AuthUser("user-9", "hero@pixelquest.test", null)), viewModel.uiState.value)
        assertEquals("user-9", profiles.storedSupabaseUserId)
        assertFalse("The form closes", viewModel.emailState.value.isOpen)
    }

    @Test
    fun aWrongOrExpiredCode_saysSo_andStaysSignedOut() = runTest(dispatcher) {
        viewModel.onEmailChanged("hero@pixelquest.test")
        viewModel.sendEmailCode()
        runCurrent()

        viewModel.onCodeChanged("12")
        viewModel.verifyEmailCode()
        assertEquals(EmailSignIn.INVALID_CODE, viewModel.emailState.value.error)
        assertTrue("Too short to send", auth.tried.isEmpty())

        viewModel.onCodeChanged("654321")
        viewModel.verifyEmailCode()
        runCurrent()

        assertTrue(viewModel.emailState.value.error!!.contains("wrong or has expired"))
        assertTrue(viewModel.uiState.value is AuthUiState.SignedOut)
        assertNull(profiles.storedSupabaseUserId)
    }

    @Test
    fun anotherCode_canBeAskedFor_onlyAfterAMinute() = runTest(dispatcher) {
        viewModel.onEmailChanged("hero@pixelquest.test")
        viewModel.sendEmailCode()
        runCurrent()
        assertEquals(EmailSignIn.RESEND_AFTER_SECONDS, viewModel.emailState.value.resendInSeconds)

        viewModel.sendEmailCode()
        runCurrent()
        assertEquals("Not again straight away", 1, auth.sentTo.size)

        advanceTimeBy(30_500)
        assertEquals(30L, viewModel.emailState.value.resendInSeconds)
        advanceTimeBy(30_000)
        assertEquals(0L, viewModel.emailState.value.resendInSeconds)

        viewModel.sendEmailCode()
        runCurrent()
        assertEquals(2, auth.sentTo.size)
    }

    @Test
    fun sendFailures_sayWhatToDo() = runTest(dispatcher) {
        val cases = mapOf(
            SupabaseResult.ServerError(429, "over_email_send_rate_limit") to "Wait a minute",
            SupabaseResult.ServerError(400, "email_address_not_authorized") to "can't be sent to this address",
            SupabaseResult.ServerError(422, "otp_disabled") to "switched off",
            SupabaseResult.NetworkError(IOException("offline")) to "Check your connection"
        )
        for ((result, expected) in cases) {
            auth.sendResult = result
            viewModel.onEmailChanged("hero@pixelquest.test")
            viewModel.sendEmailCode()
            runCurrent()
            val state = viewModel.emailState.value
            assertTrue("$result -> ${state.error}", state.error!!.contains(expected))
            assertNull("No code was sent", state.codeSentTo)
        }
    }

    @Test
    fun useDifferentEmail_goesBackToTheAddress() = runTest(dispatcher) {
        viewModel.onEmailChanged("hero@pixelquest.test")
        viewModel.sendEmailCode()
        runCurrent()

        viewModel.useDifferentEmail()

        assertNull(viewModel.emailState.value.codeSentTo)
        assertEquals("hero@pixelquest.test", viewModel.emailState.value.email)
    }
}
