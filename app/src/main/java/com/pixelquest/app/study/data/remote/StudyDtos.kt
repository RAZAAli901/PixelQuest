package com.pixelquest.app.study.data.remote

import com.pixelquest.app.party.data.remote.parseInstant
import com.pixelquest.app.study.domain.StudyCompletion
import com.pixelquest.app.study.domain.StudyItem
import com.pixelquest.app.study.domain.StudyItemKind
import com.pixelquest.app.study.domain.StudySession
import com.pixelquest.app.study.domain.SyncState
import com.pixelquest.app.study.domain.WeekScore
import com.pixelquest.app.study.domain.WeekWinner
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDate

// A study party's rows as Supabase sends them (supabase/schema.md, Study parties), and their
// mappings to the domain models. Timestamps arrive as ISO strings with an offset; days as yyyy-MM-dd.

/** A row of public.study_items. */
@Serializable
data class StudyItemDto(
    @SerialName("id") val id: String,
    @SerialName("party_id") val partyId: String,
    @SerialName("kind") val kind: String,
    @SerialName("title") val title: String,
    @SerialName("subject") val subject: String? = null,
    @SerialName("notes") val notes: String? = null,
    @SerialName("due_at") val dueAt: String? = null,
    @SerialName("pinned") val pinned: Boolean = false,
    @SerialName("created_by") val createdBy: String? = null,
    @SerialName("created_at") val createdAt: String
)

/** What a member sends to add an item: the server fills in who and when. Only the leader may send it pinned. */
@Serializable
data class NewStudyItemDto(
    @SerialName("party_id") val partyId: String,
    @SerialName("kind") val kind: String,
    @SerialName("title") val title: String,
    @SerialName("subject") val subject: String?,
    @SerialName("notes") val notes: String?,
    @SerialName("due_at") val dueAt: String?,
    @SerialName("pinned") val pinned: Boolean
)

/** A row of public.study_completions. */
@Serializable
data class StudyCompletionDto(
    @SerialName("id") val id: Long,
    @SerialName("item_id") val itemId: String,
    @SerialName("party_id") val partyId: String,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("completed_at") val completedAt: String,
    @SerialName("local_date") val localDate: String,
    @SerialName("tz") val tz: String,
    @SerialName("client_id") val clientId: String? = null
)

/** What a member sends to tick an item off: the server fills in the party, who and when. */
@Serializable
data class NewStudyCompletionDto(
    @SerialName("item_id") val itemId: String,
    @SerialName("local_date") val localDate: String,
    @SerialName("tz") val tz: String,
    @SerialName("client_id") val clientId: String
)

/** A row of public.study_sessions. */
@Serializable
data class StudySessionDto(
    @SerialName("id") val id: Long,
    @SerialName("party_id") val partyId: String,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("minutes") val minutes: Int,
    @SerialName("local_date") val localDate: String,
    @SerialName("tz") val tz: String,
    @SerialName("client_id") val clientId: String? = null,
    @SerialName("created_at") val createdAt: String
)

/** What a member sends to log study time: the server fills in who and when. */
@Serializable
data class NewStudySessionDto(
    @SerialName("party_id") val partyId: String,
    @SerialName("minutes") val minutes: Int,
    @SerialName("local_date") val localDate: String,
    @SerialName("tz") val tz: String,
    @SerialName("client_id") val clientId: String
)

/** A row of public.study_week_scores. */
@Serializable
data class WeekScoreDto(
    @SerialName("party_id") val partyId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("week_start") val weekStart: String,
    @SerialName("points") val points: Int,
    @SerialName("item_points") val itemPoints: Int,
    @SerialName("bonus_points") val bonusPoints: Int,
    @SerialName("study_points") val studyPoints: Int,
    @SerialName("items_completed") val itemsCompleted: Int,
    @SerialName("study_minutes") val studyMinutes: Int
)

/** A row of public.study_week_winners. */
@Serializable
data class WeekWinnerDto(
    @SerialName("party_id") val partyId: String,
    @SerialName("week_start") val weekStart: String,
    @SerialName("user_id") val userId: String,
    @SerialName("points") val points: Int
)

/** close_study_week's answer: closed now, or already closed; either way, the week's winners. */
@Serializable
data class CloseWeekReply(
    @SerialName("status") val status: String,
    @SerialName("winners") val winners: List<WinnerLine> = emptyList()
) {
    @Serializable
    data class WinnerLine(
        @SerialName("user_id") val userId: String,
        @SerialName("points") val points: Int
    )
}

fun StudyItemDto.toDomain(): StudyItem = StudyItem(
    id = id,
    partyId = partyId,
    kind = StudyItemKind.fromWire(kind),
    // A party member's words: plain, clean text (S054).
    title = com.pixelquest.app.domain.validation.InputRules.shown(title, com.pixelquest.app.domain.validation.InputRules.Field.STUDY_TITLE),
    subject = com.pixelquest.app.domain.validation.InputRules.shownOrNull(subject, com.pixelquest.app.domain.validation.InputRules.Field.STUDY_SUBJECT),
    notes = com.pixelquest.app.domain.validation.InputRules.shownOrNull(notes, com.pixelquest.app.domain.validation.InputRules.Field.STUDY_NOTES),
    dueAt = dueAt?.let(::parseInstant),
    pinned = pinned,
    createdBy = createdBy,
    createdAt = parseInstant(createdAt)
)

fun StudyCompletionDto.toDomain(): StudyCompletion = StudyCompletion(
    id = id,
    clientId = clientId,
    itemId = itemId,
    partyId = partyId,
    userId = userId,
    completedAt = parseInstant(completedAt),
    localDate = LocalDate.parse(localDate),
    tz = tz,
    state = SyncState.SYNCED
)

fun StudySessionDto.toDomain(): StudySession = StudySession(
    id = id,
    clientId = clientId,
    partyId = partyId,
    userId = userId,
    minutes = minutes,
    localDate = LocalDate.parse(localDate),
    tz = tz,
    createdAt = parseInstant(createdAt),
    state = SyncState.SYNCED
)

/** The score with [username], where the reader may see it (the view itself carries none). */
fun WeekScoreDto.toDomain(username: String? = null): WeekScore = WeekScore(
    partyId = partyId,
    userId = userId,
    username = username,
    weekStart = LocalDate.parse(weekStart),
    points = points,
    itemPoints = itemPoints,
    bonusPoints = bonusPoints,
    studyPoints = studyPoints,
    itemsCompleted = itemsCompleted,
    studyMinutes = studyMinutes
)

fun WeekWinnerDto.toDomain(username: String? = null): WeekWinner =
    WeekWinner(partyId, LocalDate.parse(weekStart), userId, username, points)
