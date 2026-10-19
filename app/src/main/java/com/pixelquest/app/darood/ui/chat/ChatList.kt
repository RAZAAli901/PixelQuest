package com.pixelquest.app.darood.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.unit.dp
import com.pixelquest.app.R
import com.pixelquest.app.darood.ui.DaroodTestTags
import com.pixelquest.app.ui.theme.PixelTheme
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * A party's chat, as a messaging app shows one: the newest line at the bottom, where it opens.
 * While the reader is at the bottom, a new entry scrolls into view; once they've scrolled up to read
 * older ones, it doesn't pull them away, and a button takes them back down, with how many arrived
 * meanwhile. The player's own new entry always comes into view.
 *
 * @param items the chat's lines, oldest first ([chatItemsOf]).
 * @param usernames posters' usernames by user id.
 */
@Composable
fun ChatList(
    items: List<ChatItem>,
    usernames: Map<String, String>,
    today: LocalDate,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState()
) {
    // Newest first: with the list laid out from the bottom up, index 0 is the bottom line.
    val newestFirst = remember(items) { items.asReversed() }
    val entryKeys = remember(newestFirst) { newestFirst.filterIsInstance<EntryItem>().map { it.key } }
    val newest = newestFirst.firstOrNull() as? EntryItem

    // Whether the reader is following the newest lines. Only their own scrolling changes it: a line
    // arriving at the bottom moves the list without them, and mustn't count as scrolling away.
    var following by rememberSaveable { mutableStateOf(true) }
    LaunchedEffect(listState) {
        snapshotFlow { (listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0) to listState.isScrollInProgress }
            .collect { (atBottom, scrolling) -> if (atBottom || scrolling) following = atBottom }
    }
    LaunchedEffect(newest?.key) {
        if (newest != null && (following || newest.bubble.mine)) {
            listState.scrollToItem(0)
            following = true
        }
    }

    // What the reader has seen: everything, while they follow; the newest then, once they scroll up.
    var seenNewest by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(following, entryKeys.firstOrNull()) { if (following) seenNewest = entryKeys.firstOrNull() }
    val unseen = if (following) 0 else entryKeys.indexOf(seenNewest).coerceAtLeast(0)

    val scope = rememberCoroutineScope()
    Box(modifier = modifier) {
        LazyColumn(
            state = listState,
            reverseLayout = true,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            modifier = Modifier.fillMaxSize().testTag(DaroodTestTags.CHAT_LIST)
        ) {
            items(newestFirst, key = { it.key }) { item ->
                when (item) {
                    is DayItem -> DaySeparator(day = item.day, today = today)
                    is EntryItem -> ChatEntryRow(item = item.bubble, sender = item.bubble.entry.userId?.let(usernames::get))
                    is EventItem -> EventPill(item.event)
                }
            }
        }
        if (!following) {
            JumpToNewest(
                unseen = unseen,
                onClick = {
                    following = true
                    scope.launch { listState.animateScrollToItem(0) }
                }
            )
        }
    }
}

/** The way back down to the newest line, with how many entries arrived while the reader was away. */
@Composable
private fun BoxScope.JumpToNewest(unseen: Int, onClick: () -> Unit) {
    val colors = PixelTheme.colors
    val label = if (unseen > 0) {
        stringResource(R.string.darood_chat_jump) + ", " + pluralStringResource(R.plurals.darood_chat_unseen, unseen, unseen)
    } else {
        stringResource(R.string.darood_chat_jump)
    }
    Box(modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .testTag(DaroodTestTags.CHAT_JUMP)
                .size(48.dp)
                .clip(CircleShape)
                .background(colors.secondary)
                .clickable(role = Role.Button, onClick = onClick)
                // Read as one button, "Jump to the newest, 3 new": the arrow and the count are for the eye.
                .clearAndSetSemantics {
                    contentDescription = label
                    role = Role.Button
                    onClick { onClick(); true }
                }
        ) {
            Text(text = "▼", style = MaterialTheme.typography.titleMedium, color = colors.onSecondary)
        }
        if (unseen > 0) {
            Text(
                text = if (unseen > 99) "99+" else unseen.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = colors.onPrimary,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 4.dp, y = (-4).dp)
                    .clearAndSetSemantics { }
                    .background(colors.primary, CircleShape)
                    .padding(horizontal = 5.dp, vertical = 1.dp)
            )
        }
    }
}
