package com.pixelquest.app.chess.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pixelquest.app.R
import com.pixelquest.app.ui.components.PqTopBar
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqDimens
import com.pixelquest.app.ui.theme.PqType

/** Where a chess notification lands until the board arrives (Phase E). */
@Composable
fun ChessComingScreen(onNavigateBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(PqColors.Ground)) {
        PqTopBar(stringResource(R.string.chess_coming_title), onBack = onNavigateBack, modifier = Modifier.padding(horizontal = PqDimens.ScreenPad).padding(top = 8.dp))
        Text(text = stringResource(R.string.chess_coming_text), style = PqType.Body, modifier = Modifier.padding(24.dp))
    }
}
