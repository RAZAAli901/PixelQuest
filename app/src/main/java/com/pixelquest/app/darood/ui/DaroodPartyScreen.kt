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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pixelquest.app.R
import com.pixelquest.app.ui.components.PixelButton
import com.pixelquest.app.ui.components.PixelCard
import com.pixelquest.app.ui.components.PixelPanelVariant
import com.pixelquest.app.ui.components.PixelTopAppBar
import com.pixelquest.app.ui.components.SymbolIcon
import com.pixelquest.app.ui.theme.PixelTheme
import com.pixelquest.app.ui.theme.inkOnPanel

/**
 * Darood Party's home, from the side menu. Parties need an account: signed out, it offers the
 * app's usual sign-in ([onSignIn] opens the Account screen). Signed in, for now it says what
 * parties will be; the party list (Phase D) replaces that.
 */
@Composable
fun DaroodPartyScreen(
    onNavigateBack: (() -> Unit)?,
    onSignIn: () -> Unit,
    viewModel: DaroodPartyViewModel = hiltViewModel()
) {
    val signedIn by viewModel.signedIn.collectAsState()
    DaroodPartyContent(signedIn = signedIn, onSignIn = onSignIn, onNavigateBack = onNavigateBack)
}

/** @param signedIn null while the saved session loads: nothing is shown yet. */
@Composable
fun DaroodPartyContent(signedIn: Boolean?, onSignIn: () -> Unit, onNavigateBack: (() -> Unit)?) {
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
            when (signedIn) {
                null -> Unit
                false -> DaroodCard(
                    title = stringResource(R.string.darood_signed_out_title),
                    body = stringResource(R.string.darood_signed_out_body),
                    modifier = Modifier.testTag(DaroodTestTags.SIGNED_OUT)
                ) {
                    PixelButton(
                        text = stringResource(R.string.darood_sign_in),
                        onClick = onSignIn,
                        modifier = Modifier.fillMaxWidth().testTag(DaroodTestTags.SIGN_IN)
                    )
                }
                true -> DaroodCard(
                    title = stringResource(R.string.darood_empty_title),
                    body = stringResource(R.string.darood_empty_body),
                    modifier = Modifier.testTag(DaroodTestTags.EMPTY)
                )
            }
        }
    }
}

/** The 📿 card: a title, a line or two, and an optional action under them. */
@Composable
private fun DaroodCard(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    val colors = PixelTheme.colors
    PixelCard(variant = PixelPanelVariant.BEIGE, contentPadding = 20.dp, modifier = modifier.fillMaxWidth()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "📿", style = MaterialTheme.typography.displaySmall)
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = inkOnPanel(colors.primaryText),
                textAlign = TextAlign.Center
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurface,
                textAlign = TextAlign.Center
            )
            action?.invoke()
        }
    }
}
