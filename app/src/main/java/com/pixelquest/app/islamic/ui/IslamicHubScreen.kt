package com.pixelquest.app.islamic.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.pixelquest.app.islamic.IslamicItem
import com.pixelquest.app.ui.components.PixelTopAppBar
import com.pixelquest.app.ui.menu.LocalMenuDrawer
import com.pixelquest.app.ui.menu.MenuTopBarAction
import com.pixelquest.app.ui.theme.PixelTheme

/** The Islamic section's landing screen (docs/islamic-hub.md): a card for each item that's on. */
@Composable
fun IslamicHubScreen(
    onOpen: (route: String) -> Unit,
    viewModel: IslamicHubViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    IslamicHubContent(state = state, onOpen = onOpen)
}

/**
 * The hub: a top bar with the side menu's three-line icon (the hub is the menu's Islamic entry),
 * then the items as a grid of cards.
 */
@Composable
fun IslamicHubContent(
    state: IslamicHubUiState,
    onOpen: (route: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = PixelTheme.colors
    val hasMenu = LocalMenuDrawer.current != null
    Scaffold(
        containerColor = colors.background,
        topBar = {
            PixelTopAppBar(
                title = stringResource(R.string.islamic_title),
                navigationIcon = if (hasMenu) { { MenuTopBarAction() } } else null
            )
        },
        modifier = modifier.testTag(IslamicTestTags.HUB)
    ) { padding ->
        IslamicGrid(
            items = state.items,
            columns = 2,
            modifier = Modifier.padding(padding)
        ) { item, cellModifier ->
            IslamicItemCard(
                item = item,
                onClick = { onOpen(item.route) },
                isSimpleMode = state.isSimpleMode,
                modifier = cellModifier
            )
        }
    }
}

/**
 * The items in rows of [columns]. A row's cards share its height (the tallest one's), so the grid
 * stays even however long a title runs; the last row's gaps are left empty.
 */
@Composable
internal fun IslamicGrid(
    items: List<IslamicItem>,
    columns: Int,
    modifier: Modifier = Modifier,
    cell: @Composable (item: IslamicItem, modifier: Modifier) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items.chunked(columns).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEach { item -> cell(item, Modifier.weight(1f).fillMaxHeight()) }
                repeat(columns - row.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
        }
    }
}
