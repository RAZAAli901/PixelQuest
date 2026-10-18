package com.pixelquest.app.ui.screens.namaz

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pixelquest.app.R
import com.pixelquest.app.ui.components.PixelTopAppBar
import com.pixelquest.app.ui.components.SymbolIcon
import com.pixelquest.app.ui.theme.PixelTheme

/** Test tags for the Namaz screen's parts. */
object NamazTestTags {
    const val SCREEN = "namaz_screen"
}

/** The Namaz Tracker, opened from the side menu (docs/namaz-design.md). */
@Composable
fun NamazScreen(
    onNavigateBack: (() -> Unit)? = null,
    viewModel: NamazViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    NamazContent(state = state, onNavigateBack = onNavigateBack)
}

/** The screen for [state]: the top bar with back and the title, then the tracker's sections. */
@Composable
fun NamazContent(
    state: NamazUiState,
    onNavigateBack: (() -> Unit)? = null
) {
    val colors = PixelTheme.colors
    Scaffold(
        containerColor = colors.background,
        topBar = { NamazTopBar(onNavigateBack = onNavigateBack) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background)
                .padding(innerPadding)
                .testTag(NamazTestTags.SCREEN),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        }
    }
}

@Composable
private fun NamazTopBar(onNavigateBack: (() -> Unit)?) {
    val colors = PixelTheme.colors
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
