package com.pixelquest.app.study.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pixelquest.app.R
import com.pixelquest.app.study.domain.ReminderAudience
import com.pixelquest.app.study.domain.ReminderKind
import com.pixelquest.app.study.domain.ReminderRepeat
import com.pixelquest.app.study.domain.ReminderScope
import com.pixelquest.app.study.domain.weekStartOf
import com.pixelquest.app.ui.components.PixelTextField
import com.pixelquest.app.ui.components.PqButton
import com.pixelquest.app.ui.components.PqFieldLabel
import com.pixelquest.app.ui.components.PqSheet
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqDimens
import com.pixelquest.app.ui.theme.PqType
import com.pixelquest.app.ui.theme.pqShape
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** The Create reminder (or Edit reminder) sheet, over the party's screen. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderSheet(form: ReminderForm, today: LocalDate, zone: ZoneId, onChange: (ReminderForm) -> Unit, onSave: () -> Unit, onDismiss: () -> Unit) {
    PqSheet(onDismiss) {
        ReminderFormContent(form, today, zone, onChange, onSave)
    }
}

/**
 * A reminder's fields: what it's about, its title, when (a week, a day, or a day and time), how it
 * repeats, and who it's for. Days and weeks are picked from the next four weeks, times by the hour:
 * the simplest choice that works in every theme.
 */
@Composable
fun ReminderFormContent(form: ReminderForm, today: LocalDate, zone: ZoneId, onChange: (ReminderForm) -> Unit, onSave: () -> Unit) {
    val locale = LocalConfiguration.current.locales[0]
    val problem = if (form.triedToSave) form.problem(zone) else null
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .testTag(ReminderTags.FORM)
    ) {
        Text(
            text = stringResource(if (form.editingId == null) R.string.study_reminder_create else R.string.study_reminder_edit).uppercase(),
            style = PqType.Title.copy(color = PqColors.Title),
            modifier = Modifier.semantics { heading() }
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.selectableGroup()) {
            items(ReminderKind.entries, key = { it.name }) { kind ->
                ChoiceChip(stringResource(reminderKindLabel(kind)), form.kind == kind, Modifier.testTag(ReminderTags.kind(kind))) { onChange(form.copy(kind = kind)) }
            }
        }
        PixelTextField(
            value = form.title,
            onValueChange = { onChange(form.withTitle(it)) },
            label = stringResource(R.string.study_reminder_field_title),
            placeholder = stringResource(R.string.study_reminder_title_hint),
            errorText = if (problem == com.pixelquest.app.study.domain.ReminderProblem.TITLE) stringResource(R.string.study_reminder_problem_title) else null,
            modifier = Modifier.fillMaxWidth().testTag(ReminderTags.TITLE)
        )

        PqFieldLabel(stringResource(R.string.study_reminder_field_when))
        SegmentBar(
            options = ReminderScope.entries,
            selected = form.scope,
            label = { stringResource(scopeLabel(it)) },
            tag = ReminderTags::scope,
            onPick = { onChange(form.withScope(it)) }
        )
        when (form.scope) {
            ReminderScope.WEEK -> LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.selectableGroup()) {
                items(ReminderForm.weeks(today, form.weekStart), key = { it.toString() }) { monday ->
                    val label = when (monday) {
                        weekStartOf(today) -> stringResource(R.string.study_reminder_week_this)
                        weekStartOf(today).plusWeeks(1) -> stringResource(R.string.study_reminder_week_next)
                        else -> stringResource(R.string.study_reminder_week_of, monday.format(DateTimeFormatter.ofPattern("d MMM", locale)))
                    }
                    ChoiceChip(label, form.weekStart == monday, Modifier.testTag(ReminderTags.week(monday))) { onChange(form.copy(weekStart = monday)) }
                }
            }
            ReminderScope.DAY, ReminderScope.TIME -> {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.selectableGroup()) {
                    items(ReminderForm.days(today, form.day), key = { it.toString() }) { day ->
                        val label = when (day) {
                            today -> stringResource(R.string.study_section_today)
                            today.plusDays(1) -> stringResource(R.string.study_section_tomorrow)
                            else -> day.format(DateTimeFormatter.ofPattern("EEE d MMM", locale))
                        }
                        ChoiceChip(label, form.day == day, Modifier.testTag(ReminderTags.day(day))) { onChange(form.copy(day = day)) }
                    }
                }
                if (form.scope == ReminderScope.TIME) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.selectableGroup()) {
                        items((ReminderForm.TIMES + form.time).distinct().sorted(), key = { it.toString() }) { time ->
                            val label = time.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale))
                            ChoiceChip(label, form.time == time, Modifier.testTag(ReminderTags.time(time))) { onChange(form.copy(time = time)) }
                        }
                    }
                }
            }
        }

        PqFieldLabel(stringResource(R.string.study_reminder_field_repeat))
        SegmentBar(
            options = ReminderRepeat.entries.filter { it != ReminderRepeat.DAILY || form.scope != ReminderScope.WEEK },
            selected = form.repeat,
            label = { stringResource(repeatLabel(it)) },
            tag = ReminderTags::repeat,
            onPick = { onChange(form.copy(repeat = it)) }
        )

        PqFieldLabel(stringResource(R.string.study_reminder_field_who))
        SegmentBar(
            options = listOf(ReminderAudience.ME, ReminderAudience.PARTY),
            selected = form.audience,
            label = { stringResource(if (it == ReminderAudience.ME) R.string.study_reminder_who_me else R.string.study_reminder_who_party) },
            tag = ReminderTags::audience,
            enabled = form.editingId == null,
            onPick = { onChange(form.copy(audience = it)) }
        )
        if (form.editingId != null) Text(text = stringResource(R.string.study_reminder_who_fixed), style = PqType.Meta)

        if (problem != null && problem != com.pixelquest.app.study.domain.ReminderProblem.TITLE) {
            Text(text = stringResource(reminderProblemText(problem)), style = PqType.Meta.copy(color = PqColors.Error), modifier = Modifier.testTag(ReminderTags.PROBLEM))
        }
        PqButton(
            text = stringResource(if (form.editingId == null) R.string.study_reminder_save else R.string.study_reminder_save_changes),
            onClick = onSave,
            enabled = !form.saving,
            height = 52.dp,
            modifier = Modifier.fillMaxWidth().testTag(ReminderTags.SAVE)
        )
    }
}

/** A few choices side by side in one inked bar, the chosen one filled (as the item sheet's kind). */
@Composable
private fun <T> SegmentBar(
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    tag: (T) -> String,
    enabled: Boolean = true,
    onPick: (T) -> Unit
) {
    val shape = pqShape(12.dp)
    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(PqColors.Paper)
            .border(PqDimens.Border, PqColors.Ink, shape)
            .padding(3.dp)
            .selectableGroup()
    ) {
        options.forEach { each ->
            val on = each == selected
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp)
                    .clip(pqShape(9.dp))
                    .background(if (on) PqColors.Ink else PqColors.Paper)
                    .selectable(selected = on, enabled = enabled, role = Role.RadioButton, onClick = { onPick(each) })
                    .padding(horizontal = 4.dp)
                    .testTag(tag(each))
            ) {
                Text(text = label(each), style = PqType.MetaStrong.copy(color = if (on) PqColors.Paper else PqColors.Ink), textAlign = TextAlign.Center)
            }
        }
    }
}

fun reminderKindLabel(kind: ReminderKind): Int = when (kind) {
    ReminderKind.QUIZ -> R.string.study_reminder_kind_quiz
    ReminderKind.ASSIGNMENT -> R.string.study_reminder_kind_assignment
    ReminderKind.EXAM -> R.string.study_reminder_kind_exam
    ReminderKind.CUSTOM -> R.string.study_reminder_kind_custom
}

private fun scopeLabel(scope: ReminderScope): Int = when (scope) {
    ReminderScope.WEEK -> R.string.study_reminder_when_week
    ReminderScope.DAY -> R.string.study_reminder_when_day
    ReminderScope.TIME -> R.string.study_reminder_when_time
}

private fun repeatLabel(repeat: ReminderRepeat): Int = when (repeat) {
    ReminderRepeat.NONE -> R.string.study_reminder_repeat_none
    ReminderRepeat.DAILY -> R.string.study_reminder_repeat_daily
    ReminderRepeat.WEEKLY -> R.string.study_reminder_repeat_weekly
}

/** Test tags for the reminders' part of a study party's screen. */
object ReminderTags {
    const val ADD = "study_reminder_add"
    const val FORM = "study_reminder_form"
    const val TITLE = "study_reminder_title"
    const val PROBLEM = "study_reminder_problem"
    const val SAVE = "study_reminder_save"
    const val NOTICE = "study_reminder_notice"
    const val BANNER = "study_reminder_banner"
    fun line(id: String) = "study_reminder_line_$id"
    fun kind(kind: ReminderKind) = "study_reminder_kind_${kind.name}"
    fun scope(scope: ReminderScope) = "study_reminder_scope_${scope.name}"
    fun week(monday: LocalDate) = "study_reminder_week_$monday"
    fun day(day: LocalDate) = "study_reminder_day_$day"
    fun time(time: java.time.LocalTime) = "study_reminder_time_$time"
    fun repeat(repeat: ReminderRepeat) = "study_reminder_repeat_${repeat.name}"
    fun audience(audience: ReminderAudience) = "study_reminder_who_${audience.name}"
}
