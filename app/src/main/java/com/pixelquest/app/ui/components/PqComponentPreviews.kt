package com.pixelquest.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pixelquest.app.ui.theme.PixelQuestTheme
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqType
import com.pixelquest.app.ui.theme.ThemeMode

@Preview(name = "Buttons - Comic", group = "Components - Buttons", showBackground = true)
@Composable
fun ButtonsComicPreview() {
    PixelQuestTheme(themeMode = ThemeMode.Comic) {
        ButtonsPreviewContent()
    }
}

@Preview(name = "Buttons - Pixel", group = "Components - Buttons", showBackground = true)
@Composable
fun ButtonsPixelPreview() {
    PixelQuestTheme(themeMode = ThemeMode.Pixel) {
        ButtonsPreviewContent()
    }
}

@Composable
private fun ButtonsPreviewContent() {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PqButton("Primary Button", onClick = {}, style = PqButtonStyle.Primary)
        PqButton("Secondary Button", onClick = {}, style = PqButtonStyle.Secondary)
        PqDangerButton("Delete Quest", onClick = {}, icon = Icons.Rounded.Delete)
        PqButton("Disabled Button", onClick = {}, enabled = false)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            PqQuietButton("Quiet Action", onClick = {})
            PqIconButton(Icons.Rounded.Settings, "Settings", onClick = {})
            DismissButton("Close", onClick = {}, color = PqColors.Ink)
        }
    }
}

@Preview(name = "Cards & Surfaces - Comic", group = "Components - Cards", showBackground = true)
@Composable
fun CardsComicPreview() {
    PixelQuestTheme(themeMode = ThemeMode.Comic) {
        CardsPreviewContent()
    }
}

@Preview(name = "Cards & Surfaces - Pixel", group = "Components - Cards", showBackground = true)
@Composable
fun CardsPixelPreview() {
    PixelQuestTheme(themeMode = ThemeMode.Pixel) {
        CardsPreviewContent()
    }
}

@Composable
private fun CardsPreviewContent() {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PqCard(modifier = Modifier.padding(12.dp)) {
            Text("Default Paper Card", style = PqType.CardTitle)
            Text("This card uses the skin's default paper background and border.", style = PqType.Body)
        }
        PqCard(bg = PqColors.Gold, modifier = Modifier.padding(12.dp)) {
            Text("Gold Card / Trophy", style = PqType.CardTitle)
            Text("Celebration card for wins and milestones.", style = PqType.Body)
        }
        PqCard(bg = PqColors.Ground, modifier = Modifier.padding(12.dp)) {
            Text("Ground Card", style = PqType.CardTitle)
            Text("Nested ground card for secondary layout sections.", style = PqType.Body)
        }
    }
}

@Preview(name = "Toggles & Selection - Comic", group = "Components - Selection", showBackground = true)
@Composable
fun SelectionComicPreview() {
    PixelQuestTheme(themeMode = ThemeMode.Comic) {
        SelectionPreviewContent()
    }
}

@Preview(name = "Toggles & Selection - Pixel", group = "Components - Selection", showBackground = true)
@Composable
fun SelectionPixelPreview() {
    PixelQuestTheme(themeMode = ThemeMode.Pixel) {
        SelectionPreviewContent()
    }
}

@Composable
private fun SelectionPreviewContent() {
    var switchState by remember { mutableStateOf(true) }
    var selectedSeg by remember { mutableStateOf(0) }
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            PqSwitch(checked = switchState, onCheckedChange = { switchState = it })
            PqRadio(selected = true)
            PqRadio(selected = false)
        }
        PqSegmented(
            options = listOf("All", "Daily", "Weekly"),
            selected = selectedSeg,
            onSelect = { selectedSeg = it },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            PqChip("Gold Chip", bg = PqColors.Gold)
            PqChip("Sky Chip", bg = PqColors.Sky)
            PqTag("Active", color = PqColors.Sky)
            PqTag("Perfect", color = PqColors.StatusPerfect)
        }
    }
}

@Preview(name = "Inputs - Comic", group = "Components - Inputs", showBackground = true)
@Composable
fun InputsComicPreview() {
    PixelQuestTheme(themeMode = ThemeMode.Comic) {
        InputsPreviewContent()
    }
}

@Preview(name = "Inputs - Pixel", group = "Components - Inputs", showBackground = true)
@Composable
fun InputsPixelPreview() {
    PixelQuestTheme(themeMode = ThemeMode.Pixel) {
        InputsPreviewContent()
    }
}

@Composable
private fun InputsPreviewContent() {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PixelTextField(
            value = "Morning Run",
            onValueChange = {},
            label = "Quest Name",
            placeholder = "Enter quest name",
        )
        PixelTextField(
            value = "",
            onValueChange = {},
            label = "Empty Field",
            placeholder = "Type something here...",
        )
        PixelTextField(
            value = "Invalid input",
            onValueChange = {},
            label = "Field with Error",
            errorText = "This field cannot be empty",
        )
        PqTextField(
            value = "Custom styled form input",
            onValueChange = {},
            placeholder = "Enter text",
        )
    }
}

@Preview(name = "Navigation & Headers - Comic", group = "Components - Headers", showBackground = true)
@Composable
fun HeadersComicPreview() {
    PixelQuestTheme(themeMode = ThemeMode.Comic) {
        HeadersPreviewContent()
    }
}

@Preview(name = "Navigation & Headers - Pixel", group = "Components - Headers", showBackground = true)
@Composable
fun HeadersPixelPreview() {
    PixelQuestTheme(themeMode = ThemeMode.Pixel) {
        HeadersPreviewContent()
    }
}

@Composable
private fun HeadersPreviewContent() {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PqTopBar(title = "Quest Log", subtitle = "5 active today", onBack = {})
        PqSectionHeader(title = "Up Next", action = "See all", onAction = {})
        PqProgressBar(progress = 0.75f, target = 0.7f, modifier = Modifier.fillMaxWidth())
    }
}

@Preview(name = "Quest Rows & Avatars - Comic", group = "Components - Rows", showBackground = true)
@Composable
fun QuestRowsComicPreview() {
    PixelQuestTheme(themeMode = ThemeMode.Comic) {
        QuestRowsPreviewContent()
    }
}

@Preview(name = "Quest Rows & Avatars - Pixel", group = "Components - Rows", showBackground = true)
@Composable
fun QuestRowsPixelPreview() {
    PixelQuestTheme(themeMode = ThemeMode.Pixel) {
        QuestRowsPreviewContent()
    }
}

@Composable
private fun QuestRowsPreviewContent() {
    val sampleQuest = QuestUi(
        id = 1L,
        title = "Read Surah Al-Mulk",
        time = "21:00",
        category = QuestCategory.Learning,
        repeatLabel = "Daily",
        done = true,
        xp = 50,
    )
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        QuestRowCompact(quest = sampleQuest)
        PqListRow(
            icon = Icons.AutoMirrored.Rounded.MenuBook,
            label = "Study Preferences",
            value = "Daily",
            onClick = {},
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PixelAvatar(avatar = AvatarClass.Hero, size = 48.dp)
            PixelAvatar(avatar = AvatarClass.Mage, size = 48.dp)
            PixelAvatar(avatar = AvatarClass.Warrior, size = 48.dp)
            PixelAvatar(avatar = AvatarClass.Paladin, size = 48.dp)
            PixelAvatar(avatar = AvatarClass.Rogue, size = 48.dp)
            PixelAvatar(avatar = AvatarClass.Ranger, size = 48.dp)
        }
    }
}
