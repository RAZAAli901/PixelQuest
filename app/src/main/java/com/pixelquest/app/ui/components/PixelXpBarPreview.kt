package com.pixelquest.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pixelquest.app.ui.theme.PixelBackgroundDark
import com.pixelquest.app.ui.theme.PixelQuestTheme

@Preview(showBackground = true)
@Composable
fun PixelXpBarPreview() {
    PixelQuestTheme {
        Column(
            modifier = Modifier
                .background(PixelBackgroundDark)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 0% Fill State
            PixelXpBar(
                currentProgress = 0,
                maxProgress = 7,
                level = 1
            )

            // ~50% Fill State
            PixelXpBar(
                currentProgress = 3,
                maxProgress = 7,
                level = 2
            )

            // 100% Fill State
            PixelXpBar(
                currentProgress = 7,
                maxProgress = 7,
                level = 3
            )
        }
    }
}
