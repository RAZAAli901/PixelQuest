package com.pixelquest.app.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.pixelquest.app.ui.components.CategoryTile
import com.pixelquest.app.ui.components.PqButton
import com.pixelquest.app.ui.components.PqButtonStyle
import com.pixelquest.app.ui.components.PqCard
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqDimens
import com.pixelquest.app.ui.theme.PqType

/**
 * The first thing a new player sees: what PixelQuest is, in three lines, and the way in. Where a game
 * can be brought back (a backup file, or the phone's own automatic backup), that's offered too.
 */
@Composable
fun OnboardingWelcomeScreen(
    onStartClick: () -> Unit = {},
    /** Offers to restore a backup instead of starting a new hero; hidden when null. */
    onRestoreClick: (() -> Unit)? = null,
    restoreMessage: String? = null,
    /** "11 Oct": the newest automatic backup on the phone, offered when the database was lost. */
    autoBackupDay: String? = null,
    onAutoBackupClick: () -> Unit = {}
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxSize()
            .background(PqColors.Ground)
            .verticalScroll(rememberScrollState())
            .padding(start = PqDimens.ScreenPad, end = PqDimens.ScreenPad, top = 32.dp, bottom = 24.dp)
    ) {
        Text(text = "WELCOME, HERO", style = PqType.Title.copy(color = PqColors.Title), modifier = Modifier.semantics { heading() })
        Text(text = "PIXELQUEST", style = PqType.Hero)
        Text(text = "Turn your daily routines into quests.", style = PqType.Body.copy(color = PqColors.Ink2))
        PqCard(shadow = PqDimens.ShadowLg) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                Feature(Icons.Rounded.Checklist, PqColors.Sky, "Daily quests", "Set daily habits, with reminders at the time you choose.")
                Feature(Icons.Rounded.LocalFireDepartment, PqColors.Amber, "Build streaks", "Keep perfect days going for bonus XP.")
                Feature(Icons.Rounded.EmojiEvents, PqColors.Gold, "Level up", "Earn XP, level up your hero and look back on every level.")
            }
        }
        Box(Modifier.padding(top = 8.dp)) {
            PqButton("Start your journey", onStartClick, Modifier.fillMaxWidth(), height = 54.dp)
        }
        if (autoBackupDay != null) {
            PqButton("Restore my game from $autoBackupDay", onAutoBackupClick, Modifier.fillMaxWidth(), style = PqButtonStyle.Secondary, icon = Icons.Rounded.Restore)
        }
        if (onRestoreClick != null) {
            PqButton("I have a backup", onRestoreClick, Modifier.fillMaxWidth(), style = PqButtonStyle.Secondary, icon = Icons.Rounded.UploadFile)
        }
        restoreMessage?.let { Text(text = it, style = PqType.Meta.copy(color = PqColors.Ink)) }
    }
}

@Composable
private fun Feature(icon: ImageVector, tint: Color, title: String, body: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        CategoryTile(icon, tint)
        Column {
            Text(text = title, style = PqType.BodyStrong)
            Text(text = body, style = PqType.Meta)
        }
    }
}
