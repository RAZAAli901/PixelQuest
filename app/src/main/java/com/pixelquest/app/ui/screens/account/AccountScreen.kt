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
import androidx.hilt.navigation.compose.hiltViewModel
import com.pixelquest.app.auth.AuthViewModel
import com.pixelquest.app.ui.components.PixelButton
import com.pixelquest.app.ui.components.PixelButtonVariant
import com.pixelquest.app.ui.components.PixelCard
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
            val uiState by viewModel.uiState.collectAsState()
            val context = LocalContext.current

            if (uiState is com.pixelquest.app.auth.AuthUiState.SignedOut) {
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
                            onClick = { viewModel.signInWithGoogle(context) },
                            variant = PixelButtonVariant.YELLOW,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            if (uiState is com.pixelquest.app.auth.AuthUiState.SigningIn) {
                com.pixelquest.app.ui.components.PixelLoadingState(
                    message = "AUTHENTICATING QUEST HERO...",
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (uiState is com.pixelquest.app.auth.AuthUiState.Error) {
                val errorMsg = (uiState as com.pixelquest.app.auth.AuthUiState.Error).message
                com.pixelquest.app.ui.components.PixelErrorState(
                    errorMessage = errorMsg,
                    onRetry = { viewModel.signInWithGoogle(context) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (uiState is com.pixelquest.app.auth.AuthUiState.SignedIn) {
                val user = (uiState as com.pixelquest.app.auth.AuthUiState.SignedIn).user
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
                            onClick = {
                                accountViewModel.cancelActiveSync()
                                viewModel.signOut()
                            },
                            variant = PixelButtonVariant.BLUE,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Leaderboard Opt-In Card (Defaults to OFF)
                val accountState by accountViewModel.uiState.collectAsState()
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
                            onValueChange = { accountViewModel.onDisplayNameChanged(it) },
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
                            onClick = { accountViewModel.onOptInToggleClicked(!accountState.isOptedIn) },
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
                                    .format(java.util.Date(accountState.lastSyncTime!!))
                                Text(
                                    text = "Last synced: $formattedTime",
                                    style = PixelTypography.bodySmall,
                                    color = PixelTextWhite.copy(alpha = 0.8f)
                                )
                            }
                            if (accountState.syncMessage != null) {
                                Text(
                                    text = accountState.syncMessage ?: "",
                                    style = PixelTypography.bodySmall,
                                    color = if (accountState.syncMessage?.contains("successful", ignoreCase = true) == true) {
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
                                onClick = { accountViewModel.syncNow() },
                                enabled = !accountState.isSyncing,
                                variant = PixelButtonVariant.YELLOW,
                                modifier = Modifier.fillMaxWidth()
                            )
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
                            onClick = { accountViewModel.requestDeleteCloudAccount() },
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

        val accountState by accountViewModel.uiState.collectAsState()
        if (accountState.showConfirmDialog) {
            LeaderboardPrivacyConfirmDialog(
                displayName = accountState.displayNameInput.trim().ifBlank { "Hero" },
                onConfirm = { accountViewModel.confirmOptIn() },
                onDismiss = { accountViewModel.dismissConfirmDialog() }
            )
        }
    }
}
