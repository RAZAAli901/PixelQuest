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
    viewModel: AuthViewModel = hiltViewModel()
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

            Spacer(modifier = Modifier.weight(1f, fill = false))

            PixelButton(
                text = "⬅️ BACK TO SETTINGS",
                onClick = onNavigateBack,
                variant = PixelButtonVariant.BLUE,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
