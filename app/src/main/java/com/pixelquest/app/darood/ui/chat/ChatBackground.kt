package com.pixelquest.app.darood.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pixelquest.app.ui.theme.PqColors

/** The ground behind a party's chat (mock 4E): the screen's own, plain, so the bubbles stand off it. */
@Composable
fun ChatBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(modifier = modifier.background(PqColors.Ground), content = content)
}
