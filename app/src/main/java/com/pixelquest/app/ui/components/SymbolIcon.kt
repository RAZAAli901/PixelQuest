package com.pixelquest.app.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle

/**
 * An emoji or symbol used as a button's icon (◀, 🗑️, 🔄). TalkBack reads [contentDescription]
 * ("Back", "Delete Morning Run") instead of the symbol's Unicode name ("black left-pointing
 * triangle", "wastebasket"), and the button around it reads it once.
 */
@Composable
fun SymbolIcon(
    symbol: String,
    contentDescription: String,
    style: TextStyle,
    color: Color = Color.Unspecified
) {
    Text(
        text = symbol,
        style = style,
        color = color,
        modifier = Modifier.clearAndSetSemantics { this.contentDescription = contentDescription }
    )
}
