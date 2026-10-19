package com.pixelquest.app.darood.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.pixelquest.app.R
import com.pixelquest.app.darood.ui.DaroodTestTags
import com.pixelquest.app.ui.theme.PixelTheme

/** At most this many digits: the largest entry, 100,000, has six. */
const val AMOUNT_DIGITS = 6

/**
 * What the chat's field keeps of what was typed or pasted: digits only, at most [AMOUNT_DIGITS],
 * without leading zeros. Digits in any script count, written as 0-9 (an Urdu keyboard's ۳۱۳ is
 * 313). Anything else never gets in: a paste of "10,000 Darood" keeps 10000, and "abc" nothing.
 */
fun amountDigits(raw: String): String {
    val digits = buildString { raw.forEach { if (it.isDigit()) append(it.digitToInt()) } }
    if (digits.isEmpty()) return ""
    return digits.trimStart('0').ifEmpty { "0" }.take(AMOUNT_DIGITS)
}

/**
 * The chat's input: a number, and nothing else. There's no free text anywhere in a party's chat,
 * only how many were recited. The keyboard is the number pad, and whatever arrives (typed or
 * pasted) passes through [amountDigits].
 *
 * @param trailing what follows the field (its send button).
 */
@Composable
fun NumberInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    trailing: @Composable RowScope.() -> Unit = {}
) {
    val colors = PixelTheme.colors
    val label = stringResource(R.string.darood_input_label)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surface)
            .imePadding()
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Box(
            contentAlignment = Alignment.CenterStart,
            modifier = Modifier
                .weight(1f)
                .background(colors.background, RoundedCornerShape(24.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            if (value.isEmpty()) {
                Text(text = stringResource(R.string.darood_input_placeholder), style = MaterialTheme.typography.titleMedium, color = colors.onSurfaceVariant)
            }
            BasicTextField(
                value = value,
                onValueChange = { onValueChange(amountDigits(it)) },
                singleLine = true,
                textStyle = MaterialTheme.typography.titleMedium.copy(color = colors.onSurface),
                cursorBrush = SolidColor(colors.primary),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, autoCorrect = false, imeAction = ImeAction.Done),
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = label }
                    .testTag(DaroodTestTags.CHAT_INPUT)
            )
        }
        trailing()
    }
}
