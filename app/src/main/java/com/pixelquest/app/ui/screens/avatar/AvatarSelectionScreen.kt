package com.pixelquest.app.ui.screens.avatar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pixelquest.app.domain.AvatarCatalog
import com.pixelquest.app.domain.AvatarItem
import com.pixelquest.app.ui.components.PixelAvatarDisplay
import com.pixelquest.app.ui.components.PixelCard
import com.pixelquest.app.ui.components.PixelPanelVariant
import com.pixelquest.app.ui.theme.PixelBackgroundDark
import com.pixelquest.app.ui.theme.PixelGold
import com.pixelquest.app.ui.theme.PixelQuestTypography

@Composable
fun AvatarSelectionScreen(
    currentAvatarId: String = "avatar_hero",
    onAvatarSelected: (String) -> Unit = {},
    onBackClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PixelBackgroundDark)
            .padding(16.dp)
    ) {
        Text(
            text = "🧙 CHOOSE AVATAR",
            style = PixelQuestTypography.headlineMedium,
            color = PixelGold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(AvatarCatalog.avatars) { avatar ->
                AvatarGridItem(
                    avatar = avatar,
                    isSelected = avatar.id == currentAvatarId,
                    onSelect = { onAvatarSelected(avatar.id) }
                )
            }
        }
    }
}

import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import com.pixelquest.app.ui.theme.PixelGreen
import com.pixelquest.app.ui.theme.PixelTextWhite

@Composable
private fun AvatarGridItem(
    avatar: AvatarItem,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(
                width = if (isSelected) 4.dp else 2.dp,
                color = if (isSelected) PixelGold else PixelGold.copy(alpha = 0.3f),
                shape = shape
            )
            .clickable { onSelect() }
    ) {
        PixelCard(
            variant = if (isSelected) PixelPanelVariant.YELLOW else PixelPanelVariant.BLUE,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(16.dp)
            ) {
                PixelAvatarDisplay(
                    avatarId = avatar.id,
                    size = 64.dp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = avatar.name.uppercase(),
                    style = PixelQuestTypography.titleSmall,
                    color = if (isSelected) PixelGold else PixelTextWhite,
                    textAlign = TextAlign.Center
                )
                if (isSelected) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "★ SELECTED ★",
                        style = PixelQuestTypography.labelSmall,
                        color = PixelGreen,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
