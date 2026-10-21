package com.pixelquest.app.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.pixelquest.app.ui.components.PqBackButton
import com.pixelquest.app.ui.components.PqButton
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqDimens
import com.pixelquest.app.ui.theme.PqType

/** Onboarding's four questions, after the welcome. */
internal const val ONBOARDING_STEPS = 4

/**
 * One step of onboarding in v2's language: back (from the second step on), which step of four, the
 * question as a title, a line about it, the answer's [content], and the one red button onward.
 */
@Composable
internal fun OnboardingStepFrame(
    step: Int,
    title: String,
    body: String,
    primary: String,
    onPrimary: () -> Unit,
    onBack: () -> Unit,
    primaryEnabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxSize()
            .background(PqColors.Ground)
            .imePadding()
            .padding(start = PqDimens.ScreenPad, end = PqDimens.ScreenPad, top = 8.dp, bottom = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            PqBackButton(onBack)
            Box(Modifier.weight(1f))
            Text(text = "Step $step of $ONBOARDING_STEPS", style = PqType.MetaStrong)
        }
        Text(text = title.uppercase(), style = PqType.Title.copy(color = PqColors.Title), modifier = Modifier.semantics { heading() })
        Text(text = body, style = PqType.Body.copy(color = PqColors.Ink2))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth().weight(1f), content = content)
        PqButton(primary, onPrimary, Modifier.fillMaxWidth(), enabled = primaryEnabled, height = 54.dp)
    }
}
