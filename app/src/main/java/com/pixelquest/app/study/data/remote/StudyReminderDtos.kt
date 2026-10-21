package com.pixelquest.app.study.data.remote

import com.pixelquest.app.party.data.remote.parseInstant
import com.pixelquest.app.study.domain.ReminderAudience
import com.pixelquest.app.study.domain.ReminderKind
import com.pixelquest.app.study.domain.ReminderRepeat
import com.pixelquest.app.study.domain.ReminderScope
import com.pixelquest.app.study.domain.StudyReminder
import com.pixelquest.app.study.domain.StudyReminderDraft
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDate

// A study party's reminders as Supabase sends them (public.study_reminders), and their mappings.

/** A row of public.study_reminders. */
@Serializable
data class StudyReminderDto(
    @SerialName("id") val id: String,
    @SerialName("party_id") val partyId: String,
    @SerialName("created_by") val createdBy: String? = null,
    @SerialName("title") val title: String,
    @SerialName("kind") val kind: String,
    @SerialName("scope") val scope: String,
    @SerialName("due_at") val dueAt: String? = null,
    @SerialName("week_start") val weekStart: String? = null,
    @SerialName("tz") val tz: String,
    @SerialName("repeat") val repeat: String = "none",
    @SerialName("audience") val audience: String = "party",
    @SerialName("alert_offsets") val alertOffsets: List<Int>? = null,
    @SerialName("created_at") val createdAt: String
)

/** What a member sends to add a reminder: the server fills in who and when. [clientId] is the phone's own key for it. */
@Serializable
data class NewStudyReminderDto(
    @SerialName("party_id") val partyId: String,
    @SerialName("title") val title: String,
    @SerialName("kind") val kind: String,
    @SerialName("scope") val scope: String,
    @SerialName("due_at") val dueAt: String?,
    @SerialName("week_start") val weekStart: String?,
    @SerialName("tz") val tz: String,
    @SerialName("repeat") val repeat: String,
    @SerialName("audience") val audience: String,
    @SerialName("alert_offsets") val alertOffsets: List<Int>?,
    @SerialName("client_id") val clientId: String? = null
)

/** What its creator (or the leader) may change: never its party, creator or audience. */
@Serializable
data class StudyReminderEditDto(
    @SerialName("title") val title: String,
    @SerialName("kind") val kind: String,
    @SerialName("scope") val scope: String,
    @SerialName("due_at") val dueAt: String?,
    @SerialName("week_start") val weekStart: String?,
    @SerialName("tz") val tz: String,
    @SerialName("repeat") val repeat: String,
    @SerialName("alert_offsets") val alertOffsets: List<Int>?
)

fun StudyReminderDto.toDomain(): StudyReminder = StudyReminder(
    id = id,
    partyId = partyId,
    createdBy = createdBy,
    title = title,
    kind = ReminderKind.fromWire(kind),
    scope = ReminderScope.fromWire(scope),
    dueAt = dueAt?.let(::parseInstant),
    weekStart = weekStart?.let(LocalDate::parse),
    tz = tz,
    repeat = ReminderRepeat.fromWire(repeat),
    audience = ReminderAudience.fromWire(audience),
    alertOffsets = alertOffsets,
    createdAt = parseInstant(createdAt)
)

fun StudyReminderDraft.toNewDto(partyId: String, clientId: String? = null): NewStudyReminderDto = NewStudyReminderDto(
    partyId = partyId,
    title = title.trim(),
    kind = kind.wire,
    scope = scope.wire,
    dueAt = dueAt?.toString(),
    weekStart = weekStart?.toString(),
    tz = tz,
    repeat = repeat.wire,
    audience = audience.wire,
    alertOffsets = alertOffsets,
    clientId = clientId
)

fun StudyReminderDraft.toEditDto(): StudyReminderEditDto = StudyReminderEditDto(
    title = title.trim(),
    kind = kind.wire,
    scope = scope.wire,
    dueAt = dueAt?.toString(),
    weekStart = weekStart?.toString(),
    tz = tz,
    repeat = repeat.wire,
    alertOffsets = alertOffsets
)
