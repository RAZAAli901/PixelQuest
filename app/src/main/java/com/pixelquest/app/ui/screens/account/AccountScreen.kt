package com.pixelquest.app.ui.screens.account

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.Policy
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pixelquest.app.auth.AuthViewModel
import com.pixelquest.app.ui.components.PixelConfirmDialog
import com.pixelquest.app.ui.components.PixelTextField
import com.pixelquest.app.ui.components.PqButton
import com.pixelquest.app.ui.components.PqButtonStyle
import com.pixelquest.app.ui.components.PqCard
import com.pixelquest.app.ui.components.PqDangerButton
import com.pixelquest.app.ui.components.PqGroupLabel
import com.pixelquest.app.ui.components.PqTopBar
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqDimens
import com.pixelquest.app.ui.theme.PqType

/**
 * AccountScreen provides the entry point for signing into cloud services and joining the leaderboard.
 */
@Composable
fun AccountScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AuthViewModel = hiltViewModel(),
    accountViewModel: AccountViewModel = hiltViewModel()
) {
    val authState by viewModel.uiState.collectAsState()
    val emailState by viewModel.emailState.collectAsState()
    val accountState by accountViewModel.uiState.collectAsState()
    val context = LocalContext.current

    AccountContent(
        authState = authState,
        accountState = accountState,
        onSignInWithGoogle = { viewModel.signInWithGoogle(context) },
        emailState = emailState,
        onOpenEmailSignIn = { viewModel.openEmailSignIn() },
        onEmailChange = { viewModel.onEmailChanged(it) },
        onSendEmailCode = { viewModel.sendEmailCode() },
        onEmailCodeChange = { viewModel.onCodeChanged(it) },
        onVerifyEmailCode = { viewModel.verifyEmailCode() },
        onUseDifferentEmail = { viewModel.useDifferentEmail() },
        onCloseEmailSignIn = { viewModel.closeEmailSignIn() },
        onSignOut = {
            accountViewModel.cancelActiveSync()
            viewModel.signOut()
        },
        onDisplayNameChange = { accountViewModel.onDisplayNameChanged(it) },
        onOptInToggle = { accountViewModel.onOptInToggleClicked(it) },
        onConfirmOptIn = { accountViewModel.confirmOptIn() },
        onDismissOptInDialog = { accountViewModel.dismissConfirmDialog() },
        onConfirmOptOut = { accountViewModel.confirmOptOut() },
        onDismissOptOutDialog = { accountViewModel.dismissOptOutDialog() },
        onDismissOptOutNotice = { accountViewModel.dismissOptOutSuccessNotice() },
        onViewPrivacyPolicy = { accountViewModel.showPrivacyPolicy() },
        onDismissPrivacyPolicy = { accountViewModel.dismissPrivacyPolicy() },
        onSyncNow = { accountViewModel.syncNow() },
        onRequestDeleteCloudData = { accountViewModel.requestDeleteCloudAccount() },
        onProceedDeleteDoubleConfirm = { accountViewModel.proceedToDeleteDoubleConfirm() },
        onConfirmDeleteCloudData = { accountViewModel.confirmDeleteCloudAccount(viewModel) },
        onDismissDeleteDialog = { accountViewModel.dismissDeleteDialog() },
        onNavigateBack = onNavigateBack,
        modifier = modifier
    )
}

@Composable
fun AccountContent(
    authState: com.pixelquest.app.auth.AuthUiState,
    accountState: AccountUiState,
    onSignInWithGoogle: () -> Unit = {},
    emailState: com.pixelquest.app.auth.EmailSignInState = com.pixelquest.app.auth.EmailSignInState(),
    onOpenEmailSignIn: () -> Unit = {},
    onEmailChange: (String) -> Unit = {},
    onSendEmailCode: () -> Unit = {},
    onEmailCodeChange: (String) -> Unit = {},
    onVerifyEmailCode: () -> Unit = {},
    onUseDifferentEmail: () -> Unit = {},
    onCloseEmailSignIn: () -> Unit = {},
    onSignOut: () -> Unit = {},
    onDisplayNameChange: (String) -> Unit = {},
    onOptInToggle: (Boolean) -> Unit = {},
    onConfirmOptIn: () -> Unit = {},
    onDismissOptInDialog: () -> Unit = {},
    onConfirmOptOut: () -> Unit = {},
    onDismissOptOutDialog: () -> Unit = {},
    onDismissOptOutNotice: () -> Unit = {},
    onViewPrivacyPolicy: () -> Unit = {},
    onDismissPrivacyPolicy: () -> Unit = {},
    onSyncNow: () -> Unit = {},
    onRequestDeleteCloudData: () -> Unit = {},
    onProceedDeleteDoubleConfirm: () -> Unit = {},
    onConfirmDeleteCloudData: () -> Unit = {},
    onDismissDeleteDialog: () -> Unit = {},
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier,
    /** False in a build without a Supabase project: sign-in can't work there, so it isn't offered. */
    cloudAvailable: Boolean = com.pixelquest.app.domain.CloudAvailability.inThisBuild,
    /** Sign in with Google also needs the Google web client id; email-code sign-in doesn't. */
    googleAvailable: Boolean = com.pixelquest.app.domain.CloudAvailability.googleInThisBuild,
    /**
     * Email-code sign-in also needs an email service on the project, or the codes reach only its
     * team; a build made before one is set up turns it off (EMAIL_SIGN_IN=false, docs/CLOUD_SETUP.md).
     */
    emailAvailable: Boolean = com.pixelquest.app.BuildConfig.EMAIL_SIGN_IN
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxSize()
            .background(PqColors.Ground)
            .verticalScroll(rememberScrollState())
            .padding(start = PqDimens.ScreenPad, end = PqDimens.ScreenPad, top = 8.dp, bottom = 40.dp)
    ) {
        PqTopBar("Cloud & leaderboard", onBack = onNavigateBack)
        Text(
            text = when {
                !cloudAvailable -> "Cloud features are off in this build."
                // Signed in (by email or Google): no more invitation to sign in, nor to join
                // a board the player is already on (seen on the emulator).
                authState is com.pixelquest.app.auth.AuthUiState.SignedIn && accountState.isOptedIn ->
                    "You're signed in and on the leaderboard. Your streaks and level sync as you play."
                authState is com.pixelquest.app.auth.AuthUiState.SignedIn ->
                    "You're signed in. Join the leaderboard below to compare streaks with other players."
                else -> "Sign in with Google or your email to join the community quest leaderboard, sync your stats and use the AI Coach."
            },
            style = PqType.Body.copy(color = PqColors.Ink2)
        )

        // A failed sign-in (say, Google with no Google account on the phone) shows its error
        // above the sign-in card, so email sign-in is still there.
        if (authState is com.pixelquest.app.auth.AuthUiState.Error) {
            AccountProblem((authState as com.pixelquest.app.auth.AuthUiState.Error).message, onSignInWithGoogle)
        }

        if (authState is com.pixelquest.app.auth.AuthUiState.SignedOut || authState is com.pixelquest.app.auth.AuthUiState.Error) {
            AccountSection(if (cloudAvailable) "Join the leaderboard" else "Leaderboard") {
                if (cloudAvailable && (googleAvailable || emailAvailable)) {
                    Text(
                        text = "Sign in to join the leaderboard and use the AI Coach. By default, your stats remain private until you explicitly choose to opt in.",
                        style = PqType.Body.copy(color = PqColors.Ink2)
                    )
                    if (googleAvailable && !emailState.isOpen) {
                        PqButton("Sign in with Google", onSignInWithGoogle, Modifier.fillMaxWidth())
                    }
                    if (emailAvailable) {
                        EmailSignInSection(
                            state = emailState,
                            onOpen = onOpenEmailSignIn,
                            onEmailChange = onEmailChange,
                            onSendCode = onSendEmailCode,
                            onCodeChange = onEmailCodeChange,
                            onVerifyCode = onVerifyEmailCode,
                            onUseDifferentEmail = onUseDifferentEmail,
                            onClose = onCloseEmailSignIn
                        )
                    }
                } else {
                    Text(text = com.pixelquest.app.domain.CloudAvailability.NOT_IN_THIS_BUILD, style = PqType.Body.copy(color = PqColors.Ink2))
                }
                com.pixelquest.app.ui.screens.settings.SettingLink(Icons.Rounded.Policy, "Privacy policy", onViewPrivacyPolicy)
            }
        }

        if (authState is com.pixelquest.app.auth.AuthUiState.SigningIn) {
            PqCard(radius = PqDimens.RadiusLg) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                    CircularProgressIndicator(color = PqColors.Ink, strokeWidth = 3.dp, modifier = Modifier.size(28.dp))
                    Text(text = "Signing you in…", style = PqType.BodyStrong)
                }
            }
        }

        if (authState is com.pixelquest.app.auth.AuthUiState.SignedIn) {
            val signedIn = authState as com.pixelquest.app.auth.AuthUiState.SignedIn
            val user = signedIn.user
            AccountSection("Linked account") {
                if (signedIn.serverUnreachable) {
                    Text(
                        text = "Can't reach the server right now. You're still signed in: sync, the leaderboard and the AI Coach come back with the connection.",
                        style = PqType.Meta.copy(color = PqColors.Ink)
                    )
                }
                Column {
                    Text(text = "Signed in as", style = PqType.Meta)
                    Text(text = user.email ?: user.displayName ?: "Hero", style = PqType.BodyStrong)
                    Text(text = "Cloud ID ${user.id.take(8)}…${user.id.takeLast(4)}", style = PqType.Meta)
                }
                PqButton("Sign out", onSignOut, Modifier.fillMaxWidth(), style = PqButtonStyle.Secondary, icon = Icons.Rounded.Logout)
            }

            // The leaderboard: off until the player chooses it.
            AccountSection("Leaderboard") {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        Modifier.size(12.dp).clip(com.pixelquest.app.ui.theme.PqCircle)
                            .background(if (accountState.isOptedIn) PqColors.Live else PqColors.Muted)
                    )
                    Text(
                        text = if (accountState.isOptedIn) "You're on the leaderboard" else "You're not on the leaderboard",
                        style = PqType.BodyStrong
                    )
                }
                if (accountState.choiceWaiting) {
                    Text(text = "Waiting to reach the server: it's sent as soon as you're online.", style = PqType.Meta.copy(color = PqColors.Ink))
                }
                PixelTextField(
                    value = accountState.displayNameInput,
                    onValueChange = onDisplayNameChange,
                    label = "Public leaderboard name",
                    placeholder = "Enter public pseudonym",
                    errorText = accountState.displayNameError,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Shown publicly on the leaderboard. Separate from your hero name, and never your email or real name. Names are unique: nobody else can use yours while your account has it.",
                    style = PqType.Meta
                )
                Text(
                    text = "In Simple Mode, your completions, streaks and XP are still counted in the background and shown on the leaderboard.",
                    style = PqType.Meta
                )
                if (accountState.isOptedIn) {
                    PqButton("Leave the leaderboard", { onOptInToggle(false) }, Modifier.fillMaxWidth(), style = PqButtonStyle.Secondary)
                } else {
                    PqButton("Join the leaderboard", { onOptInToggle(true) }, Modifier.fillMaxWidth())
                }
            }

            if (accountState.isOptedIn) {
                AccountSection("Cloud sync") {
                    if (accountState.lastSyncTime != null) {
                        val formattedTime = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(accountState.lastSyncTime))
                        Text(text = "Last synced $formattedTime", style = PqType.Meta)
                    }
                    if (accountState.isSyncFailed) {
                        Text(text = "Cloud sync is unavailable right now. It keeps retrying in the background, and your progress is safe on this phone.", style = PqType.Meta.copy(color = PqColors.Error))
                    }
                    if (accountState.syncMessage != null) {
                        Text(
                            text = accountState.syncMessage,
                            style = PqType.MetaStrong.copy(color = if (accountState.syncMessage.contains("successful", ignoreCase = true)) PqColors.Live else PqColors.Error)
                        )
                    }
                    Text(text = "Sync runs by itself when you finish a quest or level up.", style = PqType.Meta)
                    PqButton(
                        if (accountState.isSyncing) "Syncing…" else "Sync now",
                        onSyncNow,
                        Modifier.fillMaxWidth(),
                        style = PqButtonStyle.Secondary,
                        icon = Icons.Rounded.Sync,
                        enabled = !accountState.isSyncing
                    )
                }
            }

            AccountSection("Cloud data & privacy") {
                Text(
                    text = "Permanently delete your public leaderboard profile and unlink your cloud account. Your quests and streak history on this phone stay as they are.",
                    style = PqType.Meta
                )
                PqDangerButton("Delete my cloud data", onRequestDeleteCloudData)
            }
        }
    }

    if (accountState.showConfirmDialog) {
        LeaderboardPrivacyConfirmDialog(
            displayName = accountState.displayNameInput.trim().ifBlank { "Hero" },
            onConfirm = onConfirmOptIn,
            onDismiss = onDismissOptInDialog
        )
    }

    // Lightweight Single Confirmation Dialog for Leaving Leaderboard (Opt-Out)
    if (accountState.showOptOutConfirmDialog) {
        PixelConfirmDialog(
            title = "LEAVE LEADERBOARD?",
            message = "Are you sure you want to leave the leaderboard? Your rank and public display name will no longer be visible to other players. You can rejoin at any time.",
            confirmText = "LEAVE",
            dismissText = "STAY",
            onConfirm = onConfirmOptOut,
            onDismiss = onDismissOptOutDialog
        )
    }

    // Confirmation Notice After Leaving Leaderboard
    if (accountState.showOptOutSuccessNotice) {
        com.pixelquest.app.ui.components.PixelDialog(
            title = "Left the leaderboard",
            onDismissRequest = onDismissOptOutNotice,
            confirmButtonText = "OK",
            onConfirm = onDismissOptOutNotice,
            dismissButtonText = null
        ) {
            Text(
                text = "You've left the leaderboard. Your rank and display name have been removed from public rankings.",
                style = PqType.Body.copy(color = PqColors.Ink2)
            )
        }
    }

    // Deletion Step 1: Initial Warning
    if (accountState.showDeleteConfirmDialog) {
        PixelConfirmDialog(
            title = "DELETE CLOUD DATA?",
            message = "Are you sure you want to delete your cloud account and public leaderboard record? This action cannot be undone.",
            confirmText = "CONTINUE",
            dismissText = "CANCEL",
            onConfirm = onProceedDeleteDoubleConfirm,
            onDismiss = onDismissDeleteDialog
        )
    }

    // Deletion Step 2: Final Double Confirmation
    if (accountState.showDeleteDoubleConfirmDialog) {
        PixelConfirmDialog(
            title = "FINAL WARNING: PURGE",
            message = "This permanently erases your leaderboard rank, display name, and cloud profile, and signs you out.\n\nLocal quests and streak history on this device will remain safe.",
            confirmText = "PURGE CLOUD",
            dismissText = "KEEP ACCOUNT",
            onConfirm = onConfirmDeleteCloudData,
            onDismiss = onDismissDeleteDialog
        )
    }

    // Informed Consent: In-App Privacy Information Dialog
    if (accountState.showPrivacyDialog) {
        PrivacyPolicyDialog(
            onDismiss = onDismissPrivacyPolicy
        )
    }
}

@Composable
fun PrivacyPolicyDialog(
    onDismiss: () -> Unit
) {
    com.pixelquest.app.ui.components.PixelDialog(
        title = "Privacy policy",
        onDismissRequest = onDismiss,
        confirmButtonText = "I UNDERSTAND",
        onConfirm = onDismiss,
        dismissButtonText = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 320.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "🛡️ 100% LOCAL-FIRST OPERATION",
                style = PqType.BodyStrong
            )
            Text(
                text = "By default, all your quests, schedule times, recurrence rules, completion logs, and streak history are stored strictly on your local device. We never run third-party advertising SDKs or tracking telemetry.",
                style = PqType.Meta.copy(color = PqColors.Ink2)
            )
            Text(
                text = "🏆 OPTIONAL CLOUD LEADERBOARD",
                style = PqType.BodyStrong
            )
            Text(
                text = "Leaderboard participation defaults to OFF. You can sign in with Google or with a code sent to your email; signing in alone shares nothing. If you opt in, only your public display name, level, streak, and XP are synchronized. Your individual quest descriptions and email are NEVER shared. The AI Coach is for signed-in players.",
                style = PqType.Meta.copy(color = PqColors.Ink2)
            )
            Text(
                text = "🗑️ RIGHT TO ERASE",
                style = PqType.BodyStrong
            )
            Text(
                text = "You can leave the leaderboard at any time, or permanently delete your cloud account and public record with one tap from this screen. Local progress remains safe on your device.",
                style = PqType.Meta.copy(color = PqColors.Ink2)
            )
        }
    }
}

/** A section of the account screen: its name over a card. */
@Composable
private fun AccountSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    PqGroupLabel(title, Modifier.padding(top = 8.dp).semantics { heading() })
    PqCard(radius = PqDimens.Radius, shadow = 0.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth().padding(14.dp), content = content)
    }
}

/** A sign-in that didn't work: what Android or the server said, and a way to try Google again. */
@Composable
private fun AccountProblem(message: String, onRetry: () -> Unit) {
    PqCard(radius = PqDimens.RadiusLg) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(36.dp).clip(com.pixelquest.app.ui.theme.pqShape(10.dp)).background(PqColors.Rose), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.ErrorOutline, contentDescription = null, tint = PqColors.Ink, modifier = Modifier.size(20.dp))
                }
                Text(text = "Couldn't sign in", style = PqType.BodyStrong)
            }
            Text(text = message, style = PqType.Body.copy(color = PqColors.Ink2))
            PqButton("Try again", onRetry, Modifier.fillMaxWidth(), style = PqButtonStyle.Secondary)
        }
    }
}
