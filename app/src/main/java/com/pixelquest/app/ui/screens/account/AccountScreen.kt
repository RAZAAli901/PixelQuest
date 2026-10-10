package com.pixelquest.app.ui.screens.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.pixelquest.app.auth.AuthViewModel
import com.pixelquest.app.ui.components.PixelButton
import com.pixelquest.app.ui.components.PixelButtonVariant
import com.pixelquest.app.ui.components.PixelCard
import com.pixelquest.app.ui.components.PixelConfirmDialog
import com.pixelquest.app.ui.components.PixelPanelVariant
import com.pixelquest.app.ui.theme.PixelTheme

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
    googleAvailable: Boolean = com.pixelquest.app.domain.CloudAvailability.googleInThisBuild
) {
    val colors = PixelTheme.colors

    Scaffold(
        containerColor = colors.background,
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Screen Header
            PixelCard(
                variant = PixelPanelVariant.BLUE,
                contentPadding = 16.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "☁️ CLOUD & LEADERBOARD",
                        style = MaterialTheme.typography.titleMedium,
                        color = com.pixelquest.app.ui.theme.inkOnPanel(colors.primary),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
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
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurface,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Entry Point Card
            // A failed sign-in (say, Google with no Google account on the phone) shows its error
            // above the sign-in card, so email sign-in is still there; it used to replace the card.
            if (authState is com.pixelquest.app.auth.AuthUiState.Error) {
                val errorMsg = (authState as com.pixelquest.app.auth.AuthUiState.Error).message
                com.pixelquest.app.ui.components.PixelErrorState(
                    errorMessage = errorMsg,
                    onRetry = onSignInWithGoogle,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (authState is com.pixelquest.app.auth.AuthUiState.SignedOut || authState is com.pixelquest.app.auth.AuthUiState.Error) {
                PixelCard(
                    variant = PixelPanelVariant.BEIGE,
                    contentPadding = 20.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = if (cloudAvailable) "🏆 JOIN THE LEADERBOARD" else "🏆 LEADERBOARD",
                            style = MaterialTheme.typography.titleSmall,
                            color = com.pixelquest.app.ui.theme.inkOnPanel(colors.primary),
                            textAlign = TextAlign.Center
                        )
                        if (cloudAvailable) {
                            Text(
                                text = "Sign in to join the leaderboard and use the AI Coach. By default, your stats remain private until you explicitly choose to opt in.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.onSurface,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            if (googleAvailable && !emailState.isOpen) {
                                PixelButton(
                                    text = "🌐 SIGN IN WITH GOOGLE",
                                    onClick = onSignInWithGoogle,
                                    variant = PixelButtonVariant.YELLOW,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
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
                        } else {
                            Text(
                                text = com.pixelquest.app.domain.CloudAvailability.NOT_IN_THIS_BUILD,
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.onSurface,
                                textAlign = TextAlign.Center
                            )
                        }
                        PixelButton(
                            text = "📜 VIEW PRIVACY POLICY",
                            onClick = onViewPrivacyPolicy,
                            variant = PixelButtonVariant.BLUE,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            if (authState is com.pixelquest.app.auth.AuthUiState.SigningIn) {
                com.pixelquest.app.ui.components.PixelLoadingState(
                    message = "AUTHENTICATING QUEST HERO...",
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (authState is com.pixelquest.app.auth.AuthUiState.SignedIn) {
                val user = (authState as com.pixelquest.app.auth.AuthUiState.SignedIn).user
                PixelCard(
                    variant = PixelPanelVariant.BEIGE,
                    contentPadding = 20.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "🛡️ LINKED CLOUD ACCOUNT",
                            style = MaterialTheme.typography.titleSmall,
                            color = com.pixelquest.app.ui.theme.inkOnPanel(colors.primary),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Signed in as:",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant
                        )
                        Text(
                            text = user.email ?: user.displayName ?: "Hero",
                            style = MaterialTheme.typography.bodyMedium,
                            color = com.pixelquest.app.ui.theme.inkOnPanel(colors.primary),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Cloud UID: ${user.id.take(8)}...${user.id.takeLast(4)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        PixelButton(
                            text = "🚪 SIGN OUT",
                            onClick = onSignOut,
                            variant = PixelButtonVariant.BLUE,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Leaderboard Opt-In Card (Defaults to OFF)
                PixelCard(
                    variant = PixelPanelVariant.BEIGE,
                    contentPadding = 20.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "🏆 LEADERBOARD PARTICIPATION",
                            style = MaterialTheme.typography.titleSmall,
                            color = com.pixelquest.app.ui.theme.inkOnPanel(colors.primary),
                            textAlign = TextAlign.Center
                        )
                        val optInStatusText = if (accountState.isOptedIn) {
                            "STATUS: ACTIVE (OPTED IN)"
                        } else {
                            "STATUS: INACTIVE (DEFAULT OFF)"
                        }
                        Text(
                            text = optInStatusText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = com.pixelquest.app.ui.theme.inkOnPanel(if (accountState.isOptedIn) colors.tertiary else colors.primary),
                            textAlign = TextAlign.Center
                        )
                        com.pixelquest.app.ui.components.PixelTextField(
                            value = accountState.displayNameInput,
                            onValueChange = onDisplayNameChange,
                            label = "PUBLIC LEADERBOARD NAME",
                            placeholder = "Enter public pseudonym",
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (accountState.displayNameError != null) {
                            Text(
                                text = accountState.displayNameError ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.error,
                                textAlign = TextAlign.Center
                            )
                        }
                        Text(
                            text = "ℹ️ Shown publicly on the leaderboard. Decoupled from local hero name and never shows your email or real name.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "📋 Note: When Simple Mode is active, your habit completions, streaks, and XP points are still tracked in the background and accurately reflected on the global leaderboard.",
                            style = MaterialTheme.typography.bodySmall,
                            color = com.pixelquest.app.ui.theme.inkOnPanel(colors.secondary),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        PixelButton(
                            text = if (accountState.isOptedIn) "🔴 LEAVE LEADERBOARD (OPT OUT)" else "🟢 JOIN LEADERBOARD (OPT IN)",
                            onClick = { onOptInToggle(!accountState.isOptedIn) },
                            variant = if (accountState.isOptedIn) PixelButtonVariant.BLUE else PixelButtonVariant.YELLOW,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Manual Cloud Sync Card (Foundation phase debugging & manual push)
                if (accountState.isOptedIn) {
                    PixelCard(
                        variant = PixelPanelVariant.BEIGE,
                        contentPadding = 16.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "☁️ CLOUD SYNCHRONIZATION",
                                style = MaterialTheme.typography.titleSmall,
                                color = com.pixelquest.app.ui.theme.inkOnPanel(colors.primary),
                                textAlign = TextAlign.Center
                            )
                            if (accountState.lastSyncTime != null) {
                                val formattedTime = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                                    .format(java.util.Date(accountState.lastSyncTime))
                                Text(
                                    text = "Last synced: $formattedTime",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.onSurfaceVariant
                                )
                            }
                            if (accountState.isSyncFailed) {
                                Text(
                                    text = "☁️⚠️ Cloud sync currently unavailable (retrying in background) — local progress is safe",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = com.pixelquest.app.ui.theme.inkOnPanel(colors.primary),
                                    textAlign = TextAlign.Center
                                )
                            }
                            if (accountState.syncMessage != null) {
                                Text(
                                    text = accountState.syncMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (accountState.syncMessage.contains("successful", ignoreCase = true)) {
                                        com.pixelquest.app.ui.theme.inkOnPanel(colors.tertiary)
                                    } else {
                                        com.pixelquest.app.ui.theme.inkOnPanel(colors.primary)
                                    },
                                    textAlign = TextAlign.Center
                                )
                            }
                            Text(
                                text = "ℹ️ Sync runs automatically in background on task completion & level-up.",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            PixelButton(
                                text = if (accountState.isSyncing) "⏳ SYNCING..." else "🔄 SYNC NOW",
                                onClick = onSyncNow,
                                enabled = !accountState.isSyncing,
                                variant = PixelButtonVariant.YELLOW,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // Cloud Account & Data Management
                PixelCard(
                    variant = PixelPanelVariant.BEIGE,
                    contentPadding = 16.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "🛡️ CLOUD DATA & PRIVACY",
                            style = MaterialTheme.typography.titleSmall,
                            color = com.pixelquest.app.ui.theme.inkOnPanel(colors.primary),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Permanently purge your public cloud leaderboard profile and unlink your Supabase account. Local quests and streak history remain untouched.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        PixelButton(
                            text = "🗑️ DELETE MY CLOUD DATA",
                            onClick = onRequestDeleteCloudData,
                            variant = PixelButtonVariant.YELLOW,
                            textColor = colors.error,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f, fill = false))

            PixelButton(
                text = "⬅️ BACK TO SETTINGS",
                onClick = onNavigateBack,
                variant = PixelButtonVariant.BLUE,
                modifier = Modifier.fillMaxWidth()
            )
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
                title = "LEADERBOARD",
                onDismissRequest = onDismissOptOutNotice,
                confirmButtonText = "OK",
                onConfirm = onDismissOptOutNotice,
                dismissButtonText = null
            ) {
                Text(
                    text = "You've left the leaderboard. Your rank and display name have been removed from public rankings.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurface,
                    textAlign = TextAlign.Center
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
}

@Composable
fun PrivacyPolicyDialog(
    onDismiss: () -> Unit
) {
    val colors = PixelTheme.colors
    com.pixelquest.app.ui.components.PixelDialog(
        title = "PRIVACY POLICY",
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
                style = MaterialTheme.typography.titleSmall,
                color = colors.primaryText
            )
            Text(
                text = "By default, all your quests, schedule times, recurrence rules, completion logs, and streak history are stored strictly on your local device. We never run third-party advertising SDKs or tracking telemetry.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurface
            )
            Text(
                text = "🏆 OPTIONAL CLOUD LEADERBOARD",
                style = MaterialTheme.typography.titleSmall,
                color = colors.primaryText
            )
            Text(
                text = "Leaderboard participation defaults to OFF. You can sign in with Google or with a code sent to your email; signing in alone shares nothing. If you opt in, only your public display name, level, streak, and XP are synchronized. Your individual quest descriptions and email are NEVER shared. The AI Coach is for signed-in players.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurface
            )
            Text(
                text = "🗑️ RIGHT TO ERASE",
                style = MaterialTheme.typography.titleSmall,
                color = colors.primaryText
            )
            Text(
                text = "You can leave the leaderboard at any time, or permanently delete your cloud account and public record with one tap from this screen. Local progress remains safe on your device.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurface
            )
        }
    }
}
