package com.pixelquest.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqDimens
import com.pixelquest.app.ui.theme.PqType

/**
 * A top bar, as the screens from before UI v2 ask for one: now v2's (docs/ui-v2.md), on the ground,
 * with [navigationIcon] (usually [PqBackButton]), the title in capitals, and [actions] at the end.
 *
 * [leading] (a picture before the title) and [subtitle] (a line under it) are for a header that
 * names something: its picture, its name, and a line about it.
 */
@Composable
fun PixelTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
    subtitle: String? = null,
    leading: (@Composable () -> Unit)? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .background(PqColors.Ground)
            .padding(start = PqDimens.ScreenPad, end = PqDimens.ScreenPad, top = 8.dp, bottom = 4.dp)
            .heightIn(min = 56.dp)
    ) {
        navigationIcon?.invoke()
        leading?.invoke()
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title.uppercase(),
                style = (if (subtitle == null) PqType.Title else PqType.CardTitle).copy(color = PqColors.Title),
                maxLines = if (subtitle == null) 2 else 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() }
            )
            if (subtitle != null) Text(text = subtitle, style = PqType.Meta, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (actions != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, content = actions)
        }
    }
}
