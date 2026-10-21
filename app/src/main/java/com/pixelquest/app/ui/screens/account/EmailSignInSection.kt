package com.pixelquest.app.ui.screens.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.pixelquest.app.auth.EmailSignIn
import com.pixelquest.app.auth.EmailSignInState
import com.pixelquest.app.ui.components.PixelTextField
import com.pixelquest.app.ui.components.PqButton
import com.pixelquest.app.ui.components.PqButtonStyle
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqType

/**
 * Sign in with a code sent by email, on Account's signed-out card: first the address, then the code
 * from the email. Closed, it's a single SIGN IN WITH EMAIL button.
 */
@Composable
fun EmailSignInSection(
    state: EmailSignInState,
    onOpen: () -> Unit,
    onEmailChange: (String) -> Unit,
    onSendCode: () -> Unit,
    onCodeChange: (String) -> Unit,
    onVerifyCode: () -> Unit,
    onUseDifferentEmail: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!state.isOpen) {
        PqButton("Sign in with email", onOpen, modifier.fillMaxWidth(), style = PqButtonStyle.Secondary, icon = Icons.Rounded.Email)
        return
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        val sentTo = state.codeSentTo
        if (sentTo == null) {
            Text(text = "We'll email you a sign-in code. No password needed; your account is made the first time.", style = PqType.Meta.copy(color = PqColors.Ink))
            PixelTextField(
                value = state.email,
                onValueChange = onEmailChange,
                label = "Email",
                placeholder = "you@example.com",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSendCode() }),
                modifier = Modifier.fillMaxWidth()
            )
            val canSend = state.canSendTo(state.email)
            PqButton(
                when {
                    state.isWorking -> "Sending…"
                    !canSend -> "Send code (${state.resendInSeconds}s)"
                    else -> "Send code"
                },
                onSendCode,
                Modifier.fillMaxWidth(),
                enabled = canSend
            )
        } else {
            Text(text = EmailSignIn.codeSentMessage(sentTo), style = PqType.Meta.copy(color = PqColors.Ink))
            PixelTextField(
                value = state.code,
                onValueChange = onCodeChange,
                label = "Code from the email",
                placeholder = "123456",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onVerifyCode() }),
                modifier = Modifier.fillMaxWidth()
            )
            PqButton(if (state.isWorking) "Checking…" else "Sign in", onVerifyCode, Modifier.fillMaxWidth(), enabled = !state.isWorking)
            PqButton(
                if (state.resendInSeconds > 0) "Send a new code (${state.resendInSeconds}s)" else "Send a new code",
                onSendCode,
                Modifier.fillMaxWidth(),
                style = PqButtonStyle.Secondary,
                enabled = !state.isWorking && state.resendInSeconds == 0L
            )
            PqButton("Use a different email", onUseDifferentEmail, Modifier.fillMaxWidth(), style = PqButtonStyle.Secondary, enabled = !state.isWorking)
        }
        state.error?.let { Text(text = it, style = PqType.Meta.copy(color = PqColors.Error)) }
        PqButton("Cancel", onClose, Modifier.fillMaxWidth(), style = PqButtonStyle.Secondary)
    }
}
