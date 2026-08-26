package com.pixelquest.app.ui.screens.settings

import androidx.compose.foundation.layout.fillMaxWidth
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
import com.pixelquest.app.ui.theme.PixelTypography

@Preview(showBackground = true)
@Composable
fun SettingsAccountPreview() {
    PixelQuestTheme {
        SettingsScreenScaffold(
            accountSection = {
                PixelTextField(
                    value = "DragonSlayer",
                    onValueChange = {},
                    label = "EDIT HERO NAME",
                    modifier = Modifier.fillMaxWidth()
                )
                PixelAvatarFrame(
                    avatarId = "avatar_hero",
                    level = 5,
                    size = 64.dp
                )
                PixelButton(
                    text = "🧙 CHANGE AVATAR",
                    onClick = {},
                    variant = PixelButtonVariant.BLUE,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "CURRENT DIFFICULTY: ${DifficultyMode.getDisplayName(DifficultyLevel.HARD).uppercase()}",
                    style = PixelTypography.bodyMedium,
                    color = PixelGold
                )
                PixelButton(
                    text = "🛡️ CHANGE DIFFICULTY",
                    onClick = {},
                    variant = PixelButtonVariant.YELLOW,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        )
    }
}
