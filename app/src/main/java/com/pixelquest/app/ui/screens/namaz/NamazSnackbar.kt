package com.pixelquest.app.ui.screens.namaz

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pixelquest.app.ui.components.PqButton
import com.pixelquest.app.ui.components.PqButtonStyle
import com.pixelquest.app.ui.components.PqCard
import com.pixelquest.app.ui.theme.PqDimens
import com.pixelquest.app.ui.theme.PqType

/** The Namaz screen's snackbar: a question with its answer, such as taking a prayed mark back. */
@Composable
internal fun NamazSnackbar(
    message: String,
    actionLabel: String?,
    onAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    PqCard(modifier.fillMaxWidth(), shadow = PqDimens.ShadowLg) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 8.dp)
        ) {
            Text(text = message, style = PqType.Body, modifier = Modifier.weight(1f))
            if (actionLabel != null) PqButton(text = actionLabel, onClick = onAction, style = PqButtonStyle.Secondary)
        }
    }
}
