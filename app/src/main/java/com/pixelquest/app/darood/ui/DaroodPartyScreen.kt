package com.pixelquest.app.darood.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pixelquest.app.R
import com.pixelquest.app.ui.components.PixelCard
import com.pixelquest.app.ui.components.PixelPanelVariant
import com.pixelquest.app.ui.components.PixelTopAppBar
import com.pixelquest.app.ui.components.SymbolIcon
import com.pixelquest.app.ui.theme.PixelTheme
import com.pixelquest.app.ui.theme.inkOnPanel

/**
 * Darood Party's home, from the side menu. For now it says what parties will be; the party list
 * (Phase D) replaces its body.
 */
@Composable
fun DaroodPartyScreen(onNavigateBack: (() -> Unit)?) {
    val colors = PixelTheme.colors
    Scaffold(
        containerColor = colors.background,
        topBar = {
            PixelTopAppBar(
                title = stringResource(R.string.darood_title),
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
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .testTag(DaroodTestTags.PARTY_SCREEN),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PixelCard(variant = PixelPanelVariant.BEIGE, contentPadding = 20.dp, modifier = Modifier.fillMaxWidth()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "📿", style = MaterialTheme.typography.displaySmall)
                    Text(
                        text = stringResource(R.string.darood_empty_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = inkOnPanel(colors.primaryText),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = stringResource(R.string.darood_empty_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurface,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
