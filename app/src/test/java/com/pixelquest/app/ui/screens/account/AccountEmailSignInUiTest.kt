package com.pixelquest.app.ui.screens.account

import android.app.Application
import android.content.Context
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.auth.AuthRepository
import com.pixelquest.app.auth.AuthUiState
import com.pixelquest.app.auth.AuthUser
import com.pixelquest.app.auth.AuthViewModel
import com.pixelquest.app.auth.FakeAuthRepository
import com.pixelquest.app.auth.FakeGoogleAuthManager
import com.pixelquest.app.auth.FakeUserProfileRepositoryForAuth
import com.pixelquest.app.data.remote.SupabaseResult
import com.pixelquest.app.ui.theme.PixelQuestTheme
import com.pixelquest.app.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Account's email sign-in, driven through the screen: address, code from the email, signed in. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h914dp")
class AccountEmailSignInUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private class EmailAuth : AuthRepository by FakeAuthRepository() {
        val sentTo = mutableListOf<String>()
        override suspend fun sendEmailCode(email: String): SupabaseResult<Unit> {
            sentTo += email
            return SupabaseResult.Success(Unit)
        }
        override suspend fun verifyEmailCode(email: String, code: String): SupabaseResult<AuthUser> =
            if (code == "246810") SupabaseResult.Success(AuthUser("user-7", email, null))
            else SupabaseResult.ServerError(403, "otp_expired")
    }

    @Test
    fun signingInWithAnEmailedCode_throughTheScreen() {
        val context: Context = ApplicationProvider.getApplicationContext()
        val auth = EmailAuth()
        val viewModel = AuthViewModel(FakeGoogleAuthManager(context), auth, FakeUserProfileRepositoryForAuth())

        composeTestRule.setContent {
            val authState by viewModel.uiState.collectAsState()
            val emailState by viewModel.emailState.collectAsState()
            PixelQuestTheme(themeMode = ThemeMode.Pixel) {
                AccountContent(
                    authState = authState,
                    accountState = AccountUiState(),
                    emailState = emailState,
                    onOpenEmailSignIn = viewModel::openEmailSignIn,
                    onEmailChange = viewModel::onEmailChanged,
                    onSendEmailCode = viewModel::sendEmailCode,
                    onEmailCodeChange = viewModel::onCodeChanged,
                    onVerifyEmailCode = viewModel::verifyEmailCode,
                    onUseDifferentEmail = viewModel::useDifferentEmail,
                    onCloseEmailSignIn = viewModel::closeEmailSignIn,
                    cloudAvailable = true,
                    googleAvailable = true
                )
            }
        }

        composeTestRule.onNodeWithText("✉️ SIGN IN WITH EMAIL").performScrollTo().performClick()
        composeTestRule.onNodeWithText("🌐 SIGN IN WITH GOOGLE").assertDoesNotExist() // one way at a time
        composeTestRule.onNode(hasSetTextAction()).performTextInput("hero@pixelquest.test")
        composeTestRule.onNodeWithText("📨 SEND CODE").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        assertEquals(listOf("hero@pixelquest.test"), auth.sentTo)
        composeTestRule.onNodeWithText("We emailed a sign-in code to hero@pixelquest.test", substring = true).assertExists()
        composeTestRule.onNodeWithText("📨 SEND A NEW CODE", substring = true).assertIsNotEnabled()

        composeTestRule.onNode(hasSetTextAction()).performTextInput("111111")
        composeTestRule.onNodeWithText("🔓 SIGN IN").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("wrong or has expired", substring = true).assertExists()

        viewModel.onCodeChanged("246810")
        composeTestRule.onNodeWithText("🔓 SIGN IN").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        assertTrue(viewModel.uiState.value is AuthUiState.SignedIn)
        composeTestRule.onNodeWithText("✉️ SIGN IN WITH EMAIL").assertDoesNotExist()
    }
}
