package com.pixelquest.app.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pixelquest.app.R
import com.pixelquest.app.ui.components.PqIconButton
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqDimens
import com.pixelquest.app.ui.theme.PqType

/**
 * A party's chat screen (mock 4E), the Darood chat's and the study chat's alike: its [topBar], the
 * messages over the chat's ground ([body], tagged [backgroundTag]), and the [inputBar] at the bottom.
 */
@Composable
fun ChatScaffold(
    topBar: @Composable () -> Unit,
    inputBar: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    backgroundTag: String = "chat_background",
    body: @Composable BoxScope.() -> Unit
) {
    Scaffold(containerColor = PqColors.Ground, topBar = topBar, bottomBar = inputBar, modifier = modifier) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            ChatBackground(modifier = Modifier.fillMaxWidth().weight(1f).testTag(backgroundTag), content = body)
        }
    }
}

/**
 * A chat's top bar: back, then the party's picture and name, which open its info as in a messaging
 * app ([onOpenTitle]), its [actions], and under them anything the chat keeps in view ([below]: the
 * Darood milestone, say), over an ink line.
 *
 * @param titleTag what tests find the picture and name by.
 */
@Composable
fun ChatTopBar(
    title: String,
    onNavigateBack: () -> Unit,
    onOpenTitle: () -> Unit,
    openTitleLabel: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    titleTag: String = "chat_title",
    avatar: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    below: @Composable ColumnScope.() -> Unit = {}
) {
    Column(modifier = modifier.fillMaxWidth().background(PqColors.Ground)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth().padding(start = PqDimens.ScreenPad, end = PqDimens.ScreenPad, top = 8.dp).heightIn(min = 52.dp)
        ) {
            PqIconButton(Icons.Rounded.ArrowBack, stringResource(R.string.navigate_back), onNavigateBack)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag(titleTag)
                    .clickable(role = Role.Button, onClickLabel = openTitleLabel, onClick = onOpenTitle)
            ) {
                avatar()
                Column {
                    Text(title.uppercase(), style = PqType.CardTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (subtitle != null) Text(subtitle, style = PqType.Meta, maxLines = 1)
                }
            }
            actions()
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(vertical = 8.dp), content = below)
        Box(Modifier.fillMaxWidth().height(PqDimens.BorderThin).background(PqColors.Ink))
    }
}
