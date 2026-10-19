package com.pixelquest.app.darood.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.pixelquest.app.R
import com.pixelquest.app.darood.domain.DaroodRules
import com.pixelquest.app.darood.ui.DaroodTestTags
import com.pixelquest.app.ui.theme.ComicShapeTokens
import com.pixelquest.app.ui.theme.ComicTokens
import com.pixelquest.app.ui.theme.PixelTheme
import com.pixelquest.app.ui.theme.comicBorder

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

/** From this many in one entry, the player is asked to check it before it's sent: a slip adding a zero is easy. */
const val CONFIRM_FROM = 10_000

/** Whether the number typed can be sent, judged as the server will (1 to 100,000 an entry). */
enum class DraftCheck {
    EMPTY,
    ZERO,
    TOO_MANY,
    OK;

    val canSend: Boolean get() = this == OK
}

fun checkDraft(draft: String): DraftCheck {
    if (draft.isEmpty()) return DraftCheck.EMPTY
    val amount = draft.toLongOrNull() ?: return DraftCheck.EMPTY
    return when {
        amount < DaroodRules.AMOUNT_MIN -> DraftCheck.ZERO
        amount > DaroodRules.AMOUNT_MAX -> DraftCheck.TOO_MANY
        else -> DraftCheck.OK
    }
}

/**
 * The chat's input: a number, and nothing else. There's no free text anywhere in a party's chat,
 * only how many were recited. The keyboard is the number pad, and whatever arrives (typed or
 * pasted) passes through [amountDigits].
 *
 * @param error why the number can't be sent, said just above the field; null while it can be (or
 *   while there's nothing to say, as for an empty field).
 * @param onSend sends the number: from the keyboard's send key too, but only when [checkDraft]
 *   says it can go.
 * @param quickAdd shows the usual numbers as chips above the field ([QuickAddChips]).
 * @param trailing what follows the field (its send button).
 */
@Composable
fun NumberInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null,
    onSend: () -> Unit = {},
    quickAdd: Boolean = false,
    trailing: @Composable RowScope.() -> Unit = {}
) {
    val colors = PixelTheme.colors
    val look = chatLook()
    val label = stringResource(R.string.darood_input_label)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surface)
            .imePadding()
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        if (quickAdd) QuickAddChips(onPick = { onValueChange(it.toString()) })
        if (error != null) {
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = colors.error,
                modifier = Modifier
                    .padding(start = 16.dp, bottom = 4.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite }
                    .testTag(DaroodTestTags.CHAT_INPUT_ERROR)
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Box(
                contentAlignment = Alignment.CenterStart,
                modifier = Modifier
                    .weight(1f)
                    .then(look.shadowed(look.fieldShape))
                    .background(colors.background, look.fieldShape)
                    .then(look.outlined(look.fieldShape, look.pillOutline))
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
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, autoCorrect = false, imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { if (checkDraft(value).canSend) onSend() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = label }
                        .testTag(DaroodTestTags.CHAT_INPUT)
                )
            }
            trailing()
        }
    }
}

/**
 * The round send button beside the field, as a messaging app has: lit in the theme's accent when
 * the number can go, greyed and inert when it can't (empty, 0, or too many).
 */
@Composable
fun SendButton(enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = PixelTheme.colors
    val ink = if (PixelTheme.mode == com.pixelquest.app.ui.theme.ThemeMode.Comic) {
        Modifier.comicBorder(width = ComicShapeTokens.BorderWidthThin, color = ComicTokens.SolidBlack, shape = CircleShape)
    } else {
        Modifier
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .padding(start = 8.dp)
            .testTag(DaroodTestTags.CHAT_SEND)
            .size(48.dp)
            .then(ink)
            .clip(CircleShape)
            .background(if (enabled) colors.primary else colors.surfaceVariant)
            .clickable(enabled = enabled, role = Role.Button, onClickLabel = stringResource(R.string.darood_send), onClick = onClick)
            .semantics { contentDescription = "" }
    ) {
        Text(
            text = "➤",
            style = MaterialTheme.typography.titleLarge,
            color = if (enabled) colors.onPrimary else colors.onSurfaceVariant,
            modifier = Modifier.clearAndSetSemantics { }
        )
    }
}

/** The numbers most often recited, offered as chips above the field. */
val QUICK_AMOUNTS = listOf(11, 33, 100, 313)

/**
 * Chips for the usual numbers (+11, +33, +100, +313). A tap puts its number in the field, in place
 * of what was there, and never sends it: the player still checks it and taps send.
 */
@Composable
fun QuickAddChips(onPick: (Int) -> Unit, modifier: Modifier = Modifier) {
    val colors = PixelTheme.colors
    val look = chatLook()
    // With large text they may not all fit across: the row scrolls sideways, never cut off.
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(start = 8.dp, end = 8.dp, bottom = 6.dp)
    ) {
        QUICK_AMOUNTS.forEach { amount ->
            val shape = look.pillShape
            Text(
                text = stringResource(R.string.darood_bubble_amount, com.pixelquest.app.darood.ui.formatCount(amount)),
                style = MaterialTheme.typography.labelLarge,
                color = colors.primaryText,
                modifier = Modifier
                    .testTag(DaroodTestTags.quickAdd(amount))
                    .then(look.shadowed(shape))
                    .clip(shape)
                    .background(colors.background)
                    .then(look.outlined(shape, look.pillOutline))
                    .clickable(role = Role.Button, onClickLabel = stringResource(R.string.darood_quick_fill, amount)) { onPick(amount) }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            )
        }
    }
}
