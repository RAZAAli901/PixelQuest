package com.pixelquest.app.ui.screens.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pixelquest.app.ui.components.PixelCard
import com.pixelquest.app.ui.components.PixelPanelVariant
import com.pixelquest.app.ui.theme.PixelBackgroundDark
import com.pixelquest.app.ui.theme.PixelGold
import com.pixelquest.app.ui.theme.PixelQuestTheme
import com.pixelquest.app.ui.theme.PixelTypography

@Composable
fun CreateTaskScreen(
    onNavigateBack: () -> Unit = {}
) {
    PixelQuestTheme {
        Scaffold(
            topBar = {
                PixelCard(
                    variant = PixelPanelVariant.BEIGE,
                    contentPadding = 12.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IconButton(onClick = onNavigateBack) {
                            Text("◀", style = PixelTypography.titleMedium, color = PixelGold)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "NEW QUEST",
                            style = PixelTypography.titleLarge,
                            color = PixelGold
                        )
                    }
                }
            },
            containerColor = PixelBackgroundDark
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(PixelBackgroundDark)
            ) {
                // Form content added in subsequent steps
            }
        }
    }
}
