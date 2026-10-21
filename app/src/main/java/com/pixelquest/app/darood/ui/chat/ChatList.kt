package com.pixelquest.app.darood.ui.chat

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pixelquest.app.darood.ui.DaroodTestTags
import com.pixelquest.app.ui.chat.DaySeparator
import java.time.LocalDate
import com.pixelquest.app.ui.chat.ChatTimeline
import com.pixelquest.app.ui.chat.ChatTimelineTags

/**
 * A Darood Party's chat: its days, entries and events on the shared [ChatTimeline] (newest at the
 * bottom, following, the jump button, older lines on reaching the top).
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
    ChatTimeline(
        lines = items,
        key = { it.key },
        isMessage = { it is EntryItem },
        isMine = { (it as? EntryItem)?.bubble?.mine == true },
        modifier = modifier,
        listState = listState,
        loadingOlder = loadingOlder,
        onReachOldest = onReachOldest,
        tags = ChatTimelineTags(list = DaroodTestTags.CHAT_LIST, loadingOlder = DaroodTestTags.CHAT_LOADING_OLDER, jump = DaroodTestTags.CHAT_JUMP)
    ) { item ->
        when (item) {
            is DayItem -> DaySeparator(day = item.day, today = today, tag = DaroodTestTags.day(item.day.toString()))
            is EntryItem -> ChatEntryRow(item = item.bubble, sender = item.bubble.entry.userId?.let(usernames::get), onRetry = onRetry)
            is EventItem -> EventPill(item.event)
        }
    }
}
