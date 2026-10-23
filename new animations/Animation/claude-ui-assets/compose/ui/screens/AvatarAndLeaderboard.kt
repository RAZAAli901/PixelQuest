package com.pixelquest.ui.screens

import com.pixelquest.ui.theme.*

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelquest.ui.components.*
import com.pixelquest.ui.theme.PixelQuestTheme
import com.pixelquest.ui.theme.PqColors
import com.pixelquest.ui.theme.PqDimens
import com.pixelquest.ui.theme.PqType

// =====================  CHOOSE AVATAR  =====================

/** Tap = preview. Equip button commits. */
@Composable
fun AvatarPickerScreen(
    equipped: AvatarClass,
    selected: AvatarClass,
    onBack: () -> Unit,
    onSelect: (AvatarClass) -> Unit,
    onEquip: (AvatarClass) -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
) {
    Column(
        Modifier.fillMaxSize().background(PqColors.Ground)
            .padding(start = PqDimens.ScreenPad, end = PqDimens.ScreenPad, top = 8.dp, bottom = contentPadding.calculateBottomPadding() + 40.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        PqTopBar("Choose avatar", onBack = onBack)
        PqCard(bg = PqColors.Gold, shadow = PqDimens.ShadowLg, radius = 18.dp) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                PixelAvatar(selected, 104.dp)
                Column(Modifier.padding(start = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("PREVIEW", style = PqType.Label)
                    Text(selected.label.uppercase(), style = PqType.display(40))
                    Text(if (selected == equipped) "Equipped" else "Currently equipped: ${equipped.label}", style = PqType.Body.copy(fontSize = 13.bsp))
                }
            }
        }
        LazyVerticalGrid(
            GridCells.Fixed(3), Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(top = 8.dp, end = 4.dp, bottom = 4.dp),
        ) {
            items(AvatarClass.entries) { a ->
                val sel = a == selected
                Box {
                    Column(
                        Modifier.fillMaxWidth().comicPressable({ onSelect(a) }, bg = if (sel) PqColors.Gold else PqColors.Paper, shadow = if (sel) PqDimens.Shadow else 0.dp)
                            .padding(top = 12.dp, bottom = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        PixelAvatar(a, 60.dp)
                        Text(a.label.uppercase(), style = PqType.Button.copy(fontSize = 12.lsp))
                    }
                    if (sel) Box(
                        Modifier.align(Alignment.TopEnd).offset(8.dp, (-8).dp).size(26.dp).clip(PqCircle).background(PqColors.Ink),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Rounded.Check, null, tint = PqColors.Gold, modifier = Modifier.size(18.dp)) }
                    if (a == equipped) Text(
                        "ON", style = PqType.Label.copy(fontSize = 8.lsp, color = PqColors.Paper),
                        modifier = Modifier.padding(6.dp).clip(pqShape(4.dp)).background(PqColors.Ink).padding(horizontal = 4.dp, vertical = 2.dp),
                    )
                }
            }
        }
        PqButton(
            if (selected == equipped) "Equipped" else "Equip ${selected.label}", { onEquip(selected) },
            Modifier.fillMaxWidth(), enabled = selected != equipped, height = 54.dp,
        )
    }
}

// =====================  LEADERBOARD · LOCKED  =====================

@Composable
fun LeaderboardLockedScreen(
    onBack: () -> Unit,
    onGoogleSignIn: () -> Unit,
    onEmailCode: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
) {
    Box(
        Modifier.fillMaxSize().background(PqColors.Ground)
            .padding(start = PqDimens.ScreenPad, end = PqDimens.ScreenPad, top = 8.dp, bottom = contentPadding.calculateBottomPadding())
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            PqTopBar("Leaderboard", onBack = onBack)
            // Faded preview of what unlocks
            Column(Modifier.alpha(0.4f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                (1..5).forEach { r ->
                    Row(
                        Modifier.fillMaxWidth().comic(radius = 12.dp, border = PqDimens.BorderThin, shadow = 0.dp).padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text("$r", style = PqType.number(22), modifier = Modifier.width(22.dp))
                        Box(Modifier.size(34.dp).clip(pqShape(8.dp)).background(PqColors.Ink))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Box(Modifier.fillMaxWidth(0.8f - r * 0.09f).height(10.dp).clip(PqCircle).background(PqColors.Ink))
                            Box(Modifier.fillMaxWidth(0.4f).height(8.dp).clip(PqCircle).background(PqColors.Muted))
                        }
                        Text("${2400 - r * 310} XP", style = PqType.Button)
                    }
                }
            }
        }
        PqCard(Modifier.align(Alignment.Center).fillMaxWidth(), shadow = 5.dp, radius = 18.dp) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CategoryTile(Icons.Rounded.Lock, PqColors.Gold, size = 52.dp, iconSize = 28.dp, radius = 14.dp)
                Text("HALL OF FAME LOCKED", style = PqType.display(32, 32))
                Text("Sign in to see global rankings and streaks, and to climb the board against heroes across the realm.", style = PqType.Body.copy(color = PqColors.Ink2))
                Spacer(Modifier.height(2.dp))
                PqButton("Continue with Google", onGoogleSignIn, Modifier.fillMaxWidth(), height = 50.dp)
                PqButton("Email me a code", onEmailCode, Modifier.fillMaxWidth(), style = PqButtonStyle.Secondary, height = 46.dp)
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 760)
@Composable
private fun AvatarPreview() = PixelQuestTheme { AvatarPickerScreen(AvatarClass.Hero, AvatarClass.Mage, {}, {}, {}) }

@Preview(showBackground = true, widthDp = 360, heightDp = 760)
@Composable
private fun LeaderboardPreview() = PixelQuestTheme { LeaderboardLockedScreen({}, {}, {}) }
