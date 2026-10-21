package com.pixelquest.app.darood.ui.chat

import com.pixelquest.app.ui.chat.DaySeparator
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.pixelquest.app.ui.components.comicPressable
import com.pixelquest.app.ui.theme.PqCircle
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqDimens
import com.pixelquest.app.ui.theme.PqType
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.Icon
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * A party's chat, as a messaging app shows one: the newest line at the bottom, where it opens.
 * While the reader is at the bottom, a new entry scrolls into view; once they've scrolled up to read
 * older ones, it doesn't pull them away, and a button takes them back down, with how many arrived
 * meanwhile. The player's own new entry always comes into view.
 *
 * Scrolling up to the oldest line shown asks for older ones ([onReachOldest]); while they come
 * ([loadingOlder]), a small spinner shows at the top.
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
    listState: LazyListState = rememberLazyListState(),
    loadingOlder: Boolean = false,
    onReachOldest: () -> Unit = {},
    onRetry: (String) -> Unit = {}
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

    // Near the oldest line shown (the top: the list's last few): ask for older ones.
    val reachedOldest by remember {
        derivedStateOf {
            val layout = listState.layoutInfo
            val lastShown = layout.visibleItemsInfo.lastOrNull()?.index ?: return@derivedStateOf false
            lastShown >= layout.totalItemsCount - OLDEST_MARGIN
        }
    }
    val askForOlder by rememberUpdatedState(onReachOldest)
    LaunchedEffect(reachedOldest) { if (reachedOldest) askForOlder() }

    val scope = rememberCoroutineScope()
    Box(modifier = modifier) {
        LazyColumn(
            state = listState,
            reverseLayout = true,
            contentPadding = PaddingValues(horizontal = PqDimens.ScreenPad, vertical = 12.dp),
            modifier = Modifier.fillMaxSize().testTag(DaroodTestTags.CHAT_LIST)
        ) {
            items(newestFirst, key = { it.key }) { item ->
                when (item) {
                    is DayItem -> DaySeparator(day = item.day, today = today, tag = DaroodTestTags.day(item.day.toString()))
                    is EntryItem -> ChatEntryRow(item = item.bubble, sender = item.bubble.entry.userId?.let(usernames::get), onRetry = onRetry)
                    is EventItem -> EventPill(item.event)
                }
            }
            if (loadingOlder) {
                // Last in the list, so at the top: where the older lines will appear.
                item(key = "loading-older") {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            color = PqColors.Ink,
                            modifier = Modifier.size(24.dp).testTag(DaroodTestTags.CHAT_LOADING_OLDER)
                        )
                    }
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

/** How near the oldest line shown the reader gets before older ones are asked for. */
private const val OLDEST_MARGIN = 3

/** The way back down to the newest line, with how many entries arrived while the reader was away. */
@Composable
private fun BoxScope.JumpToNewest(unseen: Int, onClick: () -> Unit) {
    val label = if (unseen > 0) {
        stringResource(R.string.chat_jump) + ", " + pluralStringResource(R.plurals.chat_unseen, unseen, unseen)
    } else {
        stringResource(R.string.chat_jump)
    }
    Box(modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .testTag(DaroodTestTags.CHAT_JUMP)
                .size(48.dp)
                .comicPressable(onClick, bg = PqColors.Sky, radius = 24.dp, shadow = 2.dp, role = Role.Button)
                // Read as one button, "Jump to the newest, 3 new": the arrow and the count are for the eye.
                .clearAndSetSemantics {
                    contentDescription = label
                    role = Role.Button
                    onClick { onClick(); true }
                }
        ) {
            Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null, tint = PqColors.Ink)
        }
        if (unseen > 0) {
            Text(
                text = if (unseen > 99) "99+" else unseen.toString(),
                style = PqType.MetaStrong.copy(color = PqColors.OnRed),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 4.dp, y = (-4).dp)
                    .clearAndSetSemantics { }
                    .background(PqColors.Red, PqCircle)
                    .border(PqDimens.BorderThin, PqColors.Ink, PqCircle)
                    .padding(horizontal = 5.dp, vertical = 1.dp)
            )
        }
    }
}
