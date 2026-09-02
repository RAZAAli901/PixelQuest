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
import com.pixelquest.app.ui.theme.PixelTypography

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
            style = PixelTypography.headlineMedium,
            color = PixelGold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        com.pixelquest.app.ui.components.PixelAvatarGrid(
            selectedAvatarId = currentAvatarId,
            onAvatarSelected = onAvatarSelected,
            modifier = Modifier.weight(1f)
        )
    }
}
