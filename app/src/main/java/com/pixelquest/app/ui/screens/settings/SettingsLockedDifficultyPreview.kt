package com.pixelquest.app.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pixelquest.app.domain.DifficultyMode
import com.pixelquest.app.domain.model.DifficultyLevel
import com.pixelquest.app.ui.components.PixelAvatarFrame
import com.pixelquest.app.ui.components.PixelButton
import com.pixelquest.app.ui.components.PixelButtonVariant
import com.pixelquest.app.ui.components.PixelTextField
import com.pixelquest.app.ui.theme.PixelGold
import com.pixelquest.app.ui.theme.PixelQuestTheme
import com.pixelquest.app.ui.theme.PixelTheme
import com.pixelquest.app.ui.theme.PixelTypography

@Preview(name = "Settings - Locked Difficulty State (Simple Mode)", showBackground = true, widthDp = 360, heightDp = 640)
@Composable
fun SettingsLockedDifficultyPreview() {
    PixelQuestTheme {
        SettingsScreenScaffold(
            accountSection = {
                PixelTextField(
                    value = "PixelHero",
                    onValueChange = {},
                    label = "USER NAME",
                    modifier = Modifier.fillMaxWidth()
                )
                PixelAvatarFrame(
                    avatarId = "avatar_hero",
                    level = 1,
                    size = 64.dp,
                    isSimpleMode = true
                )
                PixelButton(
                    text = "🧙 CHANGE AVATAR",
                    onClick = {},
                    variant = PixelButtonVariant.BLUE,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "CURRENT DIFFICULTY: ${DifficultyMode.getDisplayName(DifficultyLevel.MEDIUM).uppercase()}",
                    style = PixelTypography.bodyMedium,
                    color = PixelGold
                )
                // Disabled Difficulty Entry Point
                PixelButton(
                    text = "🛡️ CHANGE DIFFICULTY",
                    onClick = {},
                    enabled = false,
                    variant = PixelButtonVariant.YELLOW,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "🔒 Difficulty selection is locked while Simple Mode is active. Thresholds and streaks are paused.",
                    style = PixelTypography.labelSmall,
                    color = PixelTheme.colors.onSurfaceVariant
                )
            }
        )
    }
}

@Preview(name = "Settings - Difficulty Comparison (Gamified vs Locked)", showBackground = true, widthDp = 740, heightDp = 640)
@Composable
fun SettingsDifficultyComparisonPreview() {
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(modifier = Modifier.weight(1f)) {
            SettingsAccountPreview()
        }
        Box(modifier = Modifier.weight(1f)) {
            SettingsLockedDifficultyPreview()
        }
    }
}
