package com.pixelquest.app.study.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.pixelquest.app.R
import com.pixelquest.app.ui.components.PqGroupLabel
import com.pixelquest.app.ui.theme.PqType
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** The party's recent activity, at the end of This week: who added, finished and studied what, and when. */
fun LazyListScope.activityFeed(state: StudyPartyUiState, zone: ZoneId = ZoneId.systemDefault()) {
    if (state.activity.isEmpty()) return
    item(key = "activity_title") {
        PqGroupLabel(stringResource(R.string.study_activity_title), Modifier.semantics { heading() }.testTag(StudyTags.ACTIVITY))
    }
    itemsIndexed(state.activity, key = { index, line -> "activity_${line.at.toEpochMilli()}_$index" }) { _, line -> ActivityRow(line, state, zone) }
}

@Composable
private fun ActivityRow(line: ActivityLine, state: StudyPartyUiState, zone: ZoneId) {
    val username = state.members.firstOrNull { it.userId == line.userId }?.username
    val who = when {
        line.userId != null && line.userId == state.myId -> stringResource(R.string.study_activity_you)
        username != null -> stringResource(R.string.darood_username_handle, username)
        else -> stringResource(R.string.darood_someone)
    }
    val what = when (line) {
        is ActivityLine.Added -> stringResource(R.string.study_activity_added, who, line.title)
        is ActivityLine.Finished -> stringResource(R.string.study_activity_finished, who, line.title)
        is ActivityLine.Studied -> pluralStringResource(R.plurals.study_activity_studied, line.minutes, who, line.minutes)
    }
    val locale = LocalConfiguration.current.locales[0]
    val at = line.at.atZone(zone)
    val whenText = stringResource(
        R.string.study_activity_when,
        at.format(DateTimeFormatter.ofPattern("EEE d MMM", locale)),
        at.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale))
    )
    Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.fillMaxWidth()) {
        Text(text = what, style = PqType.Body)
        Text(text = whenText, style = PqType.Meta)
    }
}
