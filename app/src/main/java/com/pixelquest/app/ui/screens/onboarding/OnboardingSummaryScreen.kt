package com.pixelquest.app.ui.screens.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pixelquest.app.domain.DifficultyMode
import com.pixelquest.app.domain.model.DifficultyLevel
import com.pixelquest.app.ui.components.AvatarClass
import com.pixelquest.app.ui.components.PixelAvatar
import com.pixelquest.app.ui.components.PqCard
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqDimens
import com.pixelquest.app.ui.theme.PqType

@Composable
fun OnboardingSummaryScreen(
    username: String,
    avatarId: String,
    difficultyLevel: DifficultyLevel,
    onConfirmClick: () -> Unit,
    onBackClick: () -> Unit
) {
    val avatar = AvatarClass.from(avatarId)
    OnboardingStepFrame(
        step = 4,
        title = "Your hero",
        body = "Here's who you'll start as. You can change any of it later in Settings.",
        primary = "Begin quest",
        onPrimary = onConfirmClick,
        onBack = onBackClick
    ) {
        PqCard(bg = PqColors.Gold, shadow = PqDimens.ShadowLg, radius = 18.dp) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                PixelAvatar(avatar, 96.dp)
                Column(Modifier.padding(start = 16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(text = "LEVEL 1", style = PqType.Label)
                    Text(text = username.uppercase(), style = PqType.display(32))
                    Text(text = stringResource(avatar.label), style = PqType.Body)
                }
            }
        }
        PqCard(radius = PqDimens.Radius, shadow = 0.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                SummaryLine("Difficulty", DifficultyMode.getDisplayName(difficultyLevel))
                SummaryLine("Perfect day", "${(DifficultyMode.getPerfectDayThreshold(difficultyLevel) * 100).toInt()}% of the day's quests")
                SummaryLine("A level takes", "${DifficultyMode.getDaysRequiredPerLevel(difficultyLevel)} days")
            }
        }
    }
}

@Composable
private fun SummaryLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(text = label, style = PqType.Meta, modifier = Modifier.weight(1f))
        Text(text = value, style = PqType.MetaStrong)
    }
}
