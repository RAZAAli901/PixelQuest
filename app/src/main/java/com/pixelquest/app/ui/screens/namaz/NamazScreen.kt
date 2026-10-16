package com.pixelquest.app.ui.screens.namaz

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelquest.app.R
import com.pixelquest.app.ui.components.PixelTopAppBar
import com.pixelquest.app.ui.components.SymbolIcon
import com.pixelquest.app.ui.theme.PixelTheme

/**
 * The Namaz Tracker, opened from the side menu. A placeholder until the tracker is built (upgrade
 * plan, Phases 3-4): prayer times, a button per prayer, reminders.
 */
@Composable
fun NamazScreen(onNavigateBack: (() -> Unit)? = null) {
    val colors = PixelTheme.colors
    Scaffold(
        containerColor = colors.background,
        topBar = {
            PixelTopAppBar(
                title = stringResource(R.string.namaz_title),
                navigationIcon = onNavigateBack?.let { back ->
                    {
                        IconButton(onClick = back) {
                            SymbolIcon(
                                symbol = "◀",
                                contentDescription = stringResource(R.string.navigate_back),
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.primaryText
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background)
                .padding(innerPadding)
                .padding(horizontal = 32.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(text = "🕌", fontSize = 48.sp)
            Text(
                text = stringResource(R.string.namaz_coming_soon),
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onSurface,
                textAlign = TextAlign.Center
            )
        }
    }
}
