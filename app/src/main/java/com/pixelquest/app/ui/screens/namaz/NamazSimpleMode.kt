package com.pixelquest.app.ui.screens.namaz

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

/** Simple Mode on the Namaz screen, provided once at its root (NamazContent). */
val LocalNamazSimpleMode = staticCompositionLocalOf { false }

/** Game touches on the Namaz screen (the streak's flame, the XP a prayer earns): off in Simple Mode. */
@Composable
fun namazPlayful(): Boolean = !LocalNamazSimpleMode.current
