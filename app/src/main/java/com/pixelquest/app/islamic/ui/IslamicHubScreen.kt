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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pixelquest.app.BuildConfig
import com.pixelquest.app.R
import com.pixelquest.app.islamic.IslamicItem
import com.pixelquest.app.ui.components.PixelCard
import com.pixelquest.app.ui.components.PixelTopAppBar
import com.pixelquest.app.ui.menu.LocalMenuDrawer
import com.pixelquest.app.ui.menu.MenuTopBarAction
import com.pixelquest.app.ui.theme.PixelTheme
import com.pixelquest.app.ui.theme.inkOnPanel

/** The Islamic section's landing screen (docs/islamic-hub.md): a card for each item that's on. */
@Composable
fun IslamicHubScreen(
    onOpen: (route: String) -> Unit,
    viewModel: IslamicHubViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    IslamicHubContent(state = state, onOpen = onOpen, showComingSoon = IslamicHubFlags.showComingSoon)
}

/** Switches for working on the hub. */
object IslamicHubFlags {
    /**
     * A "More coming soon" card after the items, to see where a new item will sit. Debug builds
     * only: a release never shows it.
     */
    val showComingSoon: Boolean = BuildConfig.DEBUG
}

/** One cell of the hub's grid: an item's card, or the coming-soon card. */
private sealed interface HubCell {
    data class Item(val item: IslamicItem) : HubCell
    data object ComingSoon : HubCell
}

/**
 * The hub: a top bar with the side menu's three-line icon (the hub is the menu's Islamic entry),
 * then the items as a grid of cards.
 *
 * @param showComingSoon adds the "More coming soon" card after the items ([IslamicHubFlags]).
 */
@Composable
fun IslamicHubContent(
    state: IslamicHubUiState,
    onOpen: (route: String) -> Unit,
    modifier: Modifier = Modifier,
    showComingSoon: Boolean = false
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
        val cells = state.items.map { HubCell.Item(it) } + if (showComingSoon && state.loaded) listOf(HubCell.ComingSoon) else emptyList()
        IslamicGrid(
            cells = cells,
            columns = 2,
            modifier = Modifier.padding(padding)
        ) { cell, cellModifier ->
            when (cell) {
                is HubCell.Item -> IslamicItemCard(
                    item = cell.item,
                    onClick = { onOpen(cell.item.route) },
                    isSimpleMode = state.isSimpleMode,
                    modifier = cellModifier
                )
                HubCell.ComingSoon -> ComingSoonCard(modifier = cellModifier)
            }
        }
    }
}

/** A card that only says more is coming, where the next item will sit. It opens nothing. */
@Composable
private fun ComingSoonCard(modifier: Modifier = Modifier) {
    val colors = PixelTheme.colors
    PixelCard(contentPadding = 14.dp, modifier = modifier.testTag(IslamicTestTags.COMING_SOON)) {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(text = "✨", fontSize = 28.sp, modifier = Modifier.clearAndSetSemantics {})
            Text(
                text = stringResource(R.string.islamic_coming_soon_title),
                style = MaterialTheme.typography.titleSmall,
                color = inkOnPanel(colors.primaryText)
            )
            Text(
                text = stringResource(R.string.islamic_coming_soon_body),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurface
            )
        }
    }
}

/**
 * The cells in rows of [columns]. A row's cards share its height (the tallest one's), so the grid
 * stays even however long a title runs; the last row's gaps are left empty.
 */
@Composable
internal fun <T> IslamicGrid(
    cells: List<T>,
    columns: Int,
    modifier: Modifier = Modifier,
    cell: @Composable (cell: T, modifier: Modifier) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        cells.chunked(columns).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEach { each -> cell(each, Modifier.weight(1f).fillMaxHeight()) }
                repeat(columns - row.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
        }
    }
}
