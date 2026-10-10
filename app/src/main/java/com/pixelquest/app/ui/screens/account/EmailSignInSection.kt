package com.pixelquest.app.ui.screens.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pixelquest.app.auth.EmailSignIn
import com.pixelquest.app.auth.EmailSignInState
import com.pixelquest.app.ui.components.PixelButton
import com.pixelquest.app.ui.components.PixelButtonVariant
import com.pixelquest.app.ui.components.PixelTextField
import com.pixelquest.app.ui.theme.PixelTheme

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
    val colors = PixelTheme.colors
    if (!state.isOpen) {
        PixelButton(
            text = "✉️ SIGN IN WITH EMAIL",
            onClick = onOpen,
            variant = PixelButtonVariant.BLUE,
            modifier = modifier.fillMaxWidth()
        )
        return
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        val sentTo = state.codeSentTo
        if (sentTo == null) {
            Text(
                text = "We'll email you a sign-in code. No password needed; your account is made the first time.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurface,
                textAlign = TextAlign.Center
            )
            PixelTextField(
                value = state.email,
                onValueChange = onEmailChange,
                label = "EMAIL",
                placeholder = "you@example.com",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSendCode() }),
                modifier = Modifier.fillMaxWidth()
            )
            val canSend = state.canSendTo(state.email)
            PixelButton(
                text = when {
                    state.isWorking -> "⏳ SENDING..."
                    !canSend -> "📨 SEND CODE (${state.resendInSeconds}s)"
                    else -> "📨 SEND CODE"
                },
                onClick = onSendCode,
                enabled = canSend,
                variant = PixelButtonVariant.YELLOW,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            Text(
                text = EmailSignIn.codeSentMessage(sentTo),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurface,
                textAlign = TextAlign.Center
            )
            PixelTextField(
                value = state.code,
                onValueChange = onCodeChange,
                label = "CODE FROM THE EMAIL",
                placeholder = "123456",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onVerifyCode() }),
                modifier = Modifier.fillMaxWidth()
            )
            PixelButton(
                text = if (state.isWorking) "⏳ CHECKING..." else "🔓 SIGN IN",
                onClick = onVerifyCode,
                enabled = !state.isWorking,
                variant = PixelButtonVariant.YELLOW,
                modifier = Modifier.fillMaxWidth()
            )
            PixelButton(
                text = if (state.resendInSeconds > 0) "📨 SEND A NEW CODE (${state.resendInSeconds}s)" else "📨 SEND A NEW CODE",
                onClick = onSendCode,
                enabled = !state.isWorking && state.resendInSeconds == 0L,
                variant = PixelButtonVariant.BLUE,
                modifier = Modifier.fillMaxWidth()
            )
            PixelButton(
                text = "✏️ USE A DIFFERENT EMAIL",
                onClick = onUseDifferentEmail,
                enabled = !state.isWorking,
                variant = PixelButtonVariant.BLUE,
                modifier = Modifier.fillMaxWidth()
            )
        }
        state.error?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = colors.error,
                textAlign = TextAlign.Center
            )
        }
        PixelButton(
            text = "✖ CANCEL",
            onClick = onClose,
            variant = PixelButtonVariant.BLUE,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
