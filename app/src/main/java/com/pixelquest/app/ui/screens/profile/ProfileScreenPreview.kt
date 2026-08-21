package com.pixelquest.app.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pixelquest.app.ui.screens.ProfileScreen
import com.pixelquest.app.ui.theme.PixelBackgroundDark
import com.pixelquest.app.ui.theme.PixelQuestTheme

@Preview(showBackground = true)
@Composable
fun ProfileScreenPreview() {
    PixelQuestTheme {
        Box(
            modifier = Modifier
                .background(PixelBackgroundDark)
                .padding(16.dp)
        ) {
            ProfileScreen(
                onNavigateToDifficulty = {},
                onNavigateToLevelHistory = {}
            )
        }
    }
}
