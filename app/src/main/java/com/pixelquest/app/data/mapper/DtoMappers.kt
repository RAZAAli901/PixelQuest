package com.pixelquest.app.data.mapper

import com.pixelquest.app.data.remote.model.CloudProfileDto
import com.pixelquest.app.data.remote.model.ReportDto
import com.pixelquest.app.domain.model.LeaderboardProfile
import com.pixelquest.app.domain.model.ProfileReport

/**
 * Bidirectional mappers between network DTOs and domain models (Q042).
 */

fun CloudProfileDto.toDomain(): LeaderboardProfile = LeaderboardProfile(
    id = id,
    displayName = displayName,
    currentStreak = currentStreak,
    longestStreak = longestStreak,
    level = level,
    totalXp = totalXp,
    leaderboardOptIn = leaderboardOptIn,
    updatedAt = updatedAt,
)

fun LeaderboardProfile.toDto(): CloudProfileDto = CloudProfileDto(
    id = id,
    displayName = displayName,
    currentStreak = currentStreak,
    longestStreak = longestStreak,
    level = level,
    totalXp = totalXp,
    leaderboardOptIn = leaderboardOptIn,
    updatedAt = updatedAt,
)

fun ReportDto.toDomain(): ProfileReport = ProfileReport(
    reporterId = reporterId,
    reportedProfileId = reportedProfileId,
    reason = reason,
)

fun ProfileReport.toDto(): ReportDto = ReportDto(
    reporterId = reporterId,
    reportedProfileId = reportedProfileId,
    reason = reason,
)
