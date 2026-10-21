package com.pixelquest.app.darood.ui.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.pixelquest.app.R
import com.pixelquest.app.darood.domain.GoalRules
import com.pixelquest.app.darood.ui.formatCount
import com.pixelquest.app.darood.ui.info.MilestoneField
import com.pixelquest.app.darood.ui.info.milestoneDigits
import com.pixelquest.app.ui.components.PqButton
import com.pixelquest.app.ui.components.PqButtonStyle
import com.pixelquest.app.ui.components.PqSheet
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqType

object ExtendGoalTags {
    const val SHEET = "darood_extend_sheet"
    const val SAVE = "darood_extend_save"
    const val PROBLEM = "darood_extend_problem"
    fun quick(percent: Int) = "darood_extend_quick_$percent"
}

/**
 * The leader's Extend goal sheet (J087): the next goal, from quick options (+25%, +50%, double) on
 * the goal just reached, or any number typed (digits only). It must be above the party's [total]
 * and at most [GoalRules.MAX], as the server insists; [problem] is the server's refusal, if any.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ExtendGoalSheet(
    total: Long,
    reached: Long,
    busy: Boolean,
    problem: String?,
    onExtend: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    var typed by rememberSaveable { mutableStateOf("") }
    PqSheet(onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth().testTag(ExtendGoalTags.SHEET)) {
            Text(
                text = stringResource(R.string.darood_goal_extend).uppercase(),
                style = PqType.Title.copy(color = PqColors.Title),
                modifier = Modifier.semantics { heading() }
            )
            Text(text = stringResource(R.string.darood_goal_extend_body, formatCount(total)), style = PqType.Body)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                quickGoals(total, reached).forEach { (percent, target) ->
                    PqButton(
                        text = stringResource(if (percent == 100) R.string.darood_goal_double else R.string.darood_goal_plus, percent),
                        onClick = { typed = target.toString() },
                        style = PqButtonStyle.Secondary,
                        enabled = !busy,
                        modifier = Modifier.weight(1f).testTag(ExtendGoalTags.quick(percent))
                    )
                }
            }
            MilestoneField(typed = typed, total = total, onTypedChange = { typed = milestoneDigits(it) })
            val target = typed.toLongOrNull()
            val tooHigh = target != null && target > GoalRules.MAX
            if (tooHigh || problem != null) {
                Text(
                    text = if (tooHigh || problem == "milestone_too_high") stringResource(R.string.darood_goal_too_high, formatCount(GoalRules.MAX))
                    else stringResource(R.string.darood_goal_not_set),
                    style = PqType.Meta.copy(color = PqColors.Error),
                    modifier = Modifier.testTag(ExtendGoalTags.PROBLEM)
                )
            }
            PqButton(
                text = stringResource(R.string.darood_goal_set),
                onClick = { target?.let(onExtend) },
                enabled = !busy && target != null && target > total && !tooHigh,
                modifier = Modifier.fillMaxWidth().testTag(ExtendGoalTags.SAVE)
            )
        }
    }
}

/** +25%, +50% and double the goal just reached: each above [total], whole numbers rounded up. */
fun quickGoals(total: Long, reached: Long): List<Pair<Int, Long>> {
    val base = maxOf(reached, total, 1L)
    return listOf(25, 50, 100).map { percent ->
        val target = base + (base * percent + 99) / 100
        percent to maxOf(target, total + 1).coerceAtMost(GoalRules.MAX)
    }
}
