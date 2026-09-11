package com.pixelquest.app.ui.screens.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.pixelquest.app.ui.theme.PixelBackground
import com.pixelquest.app.ui.theme.PixelGold
import com.pixelquest.app.ui.theme.PixelTextWhite
import com.pixelquest.app.ui.theme.PixelTypography

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
    val accountState by accountViewModel.uiState.collectAsState()
    val context = LocalContext.current

    AccountContent(
        authState = authState,
        accountState = accountState,
        onSignInWithGoogle = { viewModel.signInWithGoogle(context) },
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
    onSignOut: () -> Unit = {},
    onDisplayNameChange: (String) -> Unit = {},
    onOptInToggle: (Boolean) -> Unit = {},
    onConfirmOptIn: () -> Unit = {},
    onDismissOptInDialog: () -> Unit = {},
    onConfirmOptOut: () -> Unit = {},
    onDismissOptOutDialog: () -> Unit = {},
    onDismissOptOutNotice: () -> Unit = {},
    onSyncNow: () -> Unit = {},
    onRequestDeleteCloudData: () -> Unit = {},
    onProceedDeleteDoubleConfirm: () -> Unit = {},
    onConfirmDeleteCloudData: () -> Unit = {},
    onDismissDeleteDialog: () -> Unit = {},
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Scaffold(
        containerColor = PixelBackground,
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
                        style = PixelTypography.titleMedium,
                        color = PixelGold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Connect your Google account to join the community quest leaderboard and synchronize your stats.",
                        style = PixelTypography.bodySmall,
                        color = PixelTextWhite,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Entry Point Card
            if (authState is com.pixelquest.app.auth.AuthUiState.SignedOut) {
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
                            text = "🏆 JOIN THE LEADERBOARD",
                            style = PixelTypography.titleSmall,
                            color = PixelGold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Sign in with Google to join the leaderboard. By default, your stats remain private until you explicitly choose to opt in.",
                            style = PixelTypography.bodyMedium,
                            color = PixelTextWhite,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        PixelButton(
                            text = "🌐 SIGN IN WITH GOOGLE",
                            onClick = onSignInWithGoogle,
                            variant = PixelButtonVariant.YELLOW,
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

            if (authState is com.pixelquest.app.auth.AuthUiState.Error) {
                val errorMsg = (authState as com.pixelquest.app.auth.AuthUiState.Error).message
                com.pixelquest.app.ui.components.PixelErrorState(
                    errorMessage = errorMsg,
                    onRetry = onSignInWithGoogle,
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
                            style = PixelTypography.titleSmall,
                            color = PixelGold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Signed in as:",
                            style = PixelTypography.bodySmall,
                            color = PixelTextWhite
                        )
                        Text(
                            text = user.email ?: user.displayName ?: "Hero",
                            style = PixelTypography.bodyMedium,
                            color = PixelGold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Cloud UID: ${user.id.take(8)}...${user.id.takeLast(4)}",
                            style = PixelTypography.bodySmall,
                            color = PixelTextWhite.copy(alpha = 0.7f)
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
                            style = PixelTypography.titleSmall,
                            color = PixelGold,
                            textAlign = TextAlign.Center
                        )
                        val optInStatusText = if (accountState.isOptedIn) {
                            "STATUS: ACTIVE (OPTED IN)"
                        } else {
                            "STATUS: INACTIVE (DEFAULT OFF)"
                        }
                        Text(
                            text = optInStatusText,
                            style = PixelTypography.bodyMedium,
                            color = if (accountState.isOptedIn) com.pixelquest.app.ui.theme.PixelGreen else PixelGold,
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
                                style = PixelTypography.bodySmall,
                                color = com.pixelquest.app.ui.theme.PixelRed,
                                textAlign = TextAlign.Center
                            )
                        }
                        Text(
                            text = "ℹ️ Shown publicly on the leaderboard. Decoupled from local hero name and never exposes Google email or real name.",
                            style = PixelTypography.bodySmall,
                            color = PixelTextWhite.copy(alpha = 0.8f),
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
                                style = PixelTypography.titleSmall,
                                color = PixelGold,
                                textAlign = TextAlign.Center
                            )
                            if (accountState.lastSyncTime != null) {
                                val formattedTime = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                                    .format(java.util.Date(accountState.lastSyncTime))
                                Text(
                                    text = "Last synced: $formattedTime",
                                    style = PixelTypography.bodySmall,
                                    color = PixelTextWhite.copy(alpha = 0.8f)
                                )
                            }
                            if (accountState.syncMessage != null) {
                                Text(
                                    text = accountState.syncMessage,
                                    style = PixelTypography.bodySmall,
                                    color = if (accountState.syncMessage.contains("successful", ignoreCase = true)) {
                                        com.pixelquest.app.ui.theme.PixelGreen
                                    } else {
                                        PixelGold
                                    },
                                    textAlign = TextAlign.Center
                                )
                            }
                            Text(
                                text = "ℹ️ Sync runs automatically in background on task completion & level-up.",
                                style = PixelTypography.bodySmall,
                                color = PixelTextWhite.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                            PixelButton(
                                text = if (accountState.isSyncing) "⏳ SYNCING..." else "🔄 FORCE SYNC NOW (DEBUG)",
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
                            style = PixelTypography.titleSmall,
                            color = PixelGold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Permanently purge your public cloud leaderboard profile and unlink your Supabase account. Local quests and streak history remain untouched.",
                            style = PixelTypography.bodySmall,
                            color = PixelTextWhite.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        PixelButton(
                            text = "🗑️ DELETE MY CLOUD DATA",
                            onClick = onRequestDeleteCloudData,
                            variant = PixelButtonVariant.RED,
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
                    style = PixelTypography.bodyMedium,
                    color = PixelTextWhite,
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
    }
}
