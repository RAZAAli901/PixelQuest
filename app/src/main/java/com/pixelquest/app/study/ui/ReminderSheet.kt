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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
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
fun ReminderSheet(
    form: ReminderForm,
    today: LocalDate,
    zone: ZoneId,
    onChange: (ReminderForm) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
    onDelete: () -> Unit = {}
) {
    PqSheet(onDismiss) {
        ReminderFormContent(form, today, zone, onChange, onSave, onDelete)
    }
}

/**
 * A reminder's fields: what it's about, its title, when (a week, a day, or a day and time), how it
 * repeats, and who it's for. Days and weeks are picked from the next four weeks, times by the hour:
 * the simplest choice that works in every theme.
 */
@Composable
fun ReminderFormContent(
    form: ReminderForm,
    today: LocalDate,
    zone: ZoneId,
    onChange: (ReminderForm) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit = {}
) {
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

        AlertChoice(form, onChange)

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
        if (form.editingId != null) {
            PqButton(
                text = stringResource(R.string.study_reminder_delete),
                onClick = onDelete,
                style = com.pixelquest.app.ui.components.PqButtonStyle.Secondary,
                enabled = !form.saving,
                modifier = Modifier.fillMaxWidth().testTag(ReminderTags.DELETE)
            )
        }
    }
}

/**
 * The alerts: the usual times for its kind of When, or the creator's own, up to four, each a
 * choice that's on or off. Someone else's reminder (the leader's edit) shows them, fixed.
 */
@Composable
private fun AlertChoice(form: ReminderForm, onChange: (ReminderForm) -> Unit) {
    PqFieldLabel(stringResource(R.string.study_reminder_field_alerts))
    if (form.alertsLocked) {
        Text(text = stringResource(R.string.study_reminder_alerts_locked), style = PqType.Meta, modifier = Modifier.testTag(ReminderTags.ALERTS_LOCKED))
        return
    }
    val custom = form.alertOffsets.orEmpty()
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item(key = "usual") {
            ChoiceChip(stringResource(R.string.study_reminder_alerts_default), form.alertOffsets == null, Modifier.testTag(ReminderTags.ALERTS_DEFAULT)) {
                onChange(form.withDefaultAlerts())
            }
        }
        items((ReminderForm.ALERT_CHOICES + custom).distinct().sorted(), key = { "alert_$it" }) { minutes ->
            val on = minutes in custom
            ToggleChip(alertLabel(minutes), on, enabled = on || custom.size < com.pixelquest.app.study.domain.ReminderRules.ALERTS_MAX, Modifier.testTag(ReminderTags.alert(minutes))) {
                onChange(form.toggleAlert(minutes))
            }
        }
    }
    val note = when {
        form.alertOffsets != null -> R.string.study_reminder_alerts_custom
        form.scope == ReminderScope.WEEK -> R.string.study_reminder_alerts_default_week
        form.scope == ReminderScope.DAY -> R.string.study_reminder_alerts_default_day
        else -> R.string.study_reminder_alerts_default_time
    }
    Text(text = stringResource(note), style = PqType.Meta)
}

/** One choice that's on or off, of several that can all be on: filled with ink when on. */
@Composable
private fun ToggleChip(label: String, on: Boolean, enabled: Boolean, modifier: Modifier = Modifier, onToggle: () -> Unit) {
    Text(
        text = label,
        style = PqType.MetaStrong.copy(color = if (on) PqColors.Paper else if (enabled) PqColors.Ink else PqColors.Muted),
        maxLines = 1,
        modifier = modifier
            .studyChip(if (on) PqColors.Ink else PqColors.Paper)
            .toggleable(value = on, enabled = enabled, role = Role.Checkbox, onValueChange = { onToggle() })
            .heightIn(min = 40.dp)
            .padding(horizontal = 14.dp, vertical = 11.dp)
    )
}

/** "When it starts", "30 minutes before", "2 hours before", "1 day before", "A week before". */
@Composable
fun alertLabel(minutes: Int): String = when {
    minutes == 0 -> stringResource(R.string.study_reminder_alert_at_start)
    minutes == 7 * 24 * 60 -> stringResource(R.string.study_reminder_alert_week)
    minutes % (24 * 60) == 0 -> pluralStringResource(R.plurals.study_reminder_alert_days, minutes / (24 * 60), minutes / (24 * 60))
    minutes % 60 == 0 -> pluralStringResource(R.plurals.study_reminder_alert_hours, minutes / 60, minutes / 60)
    else -> pluralStringResource(R.plurals.study_reminder_alert_minutes, minutes, minutes)
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
    const val DELETE = "study_reminder_delete"
    const val ALERTS_DEFAULT = "study_reminder_alerts_default"
    const val ALERTS_LOCKED = "study_reminder_alerts_locked"
    fun alert(minutes: Int) = "study_reminder_alert_$minutes"
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
