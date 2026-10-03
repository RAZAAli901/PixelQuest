package com.pixelquest.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

/**
 * Text colour for text that sits on a coloured panel. Comic prints all such text in black ink,
 * because its accent colours (coral-red, sky-blue, lavender, gold) are unreadable on its own
 * orange, sky-blue and white panels (as low as 1:1). Pixel and Light keep [accent].
 */
@Composable
@ReadOnlyComposable
fun inkOnPanel(accent: Color): Color =
    if (LocalAppThemeMode.current == ThemeMode.Comic) ComicTokens.SolidBlack else accent
