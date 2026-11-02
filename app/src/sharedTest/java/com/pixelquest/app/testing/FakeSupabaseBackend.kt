package com.pixelquest.app.testing

import com.pixelquest.app.data.remote.model.CloudProfileDto
import com.pixelquest.app.party.domain.MemberStatus
import com.pixelquest.app.party.domain.PartyMember
import com.pixelquest.app.party.domain.PartyRole
import java.time.Instant

/**
 * In-memory simulated Supabase backend with Row-Level Security (RLS) enforcement.
 * Mirrors Postgres policies specified in supabase/schema.md so unit and integration tests
 * can verify that unauthorized callers cannot read or modify another user's rows.
 */
class FakeSupabaseBackend(
    var currentUserId: String? = null,
) {
    // In-memory tables
    val profiles = mutableMapOf<String, CloudProfileDto>()
    val usernames = mutableMapOf<String, String>()
    val partyMembers = mutableListOf<PartyMember>()
    val reports = mutableListOf<ReportRecord>()

    data class ReportRecord(
        val reporterId: String,
        val reportedUserId: String,
        val reason: String,
        val createdAt: Instant = Instant.now(),
    )

    fun authenticate(userId: String?) {
        currentUserId = userId
    }

    /**
     * SELECT on public.profiles:
     * Enforces policy: authenticated users can read opted-in profiles or their own profile.
     * Anonymous callers cannot read profiles.
     */
    fun getProfile(id: String): CloudProfileDto? {
        val uid = currentUserId ?: throw SecurityException("RLS Violation: Anonymous users cannot read profiles")
        val profile = profiles[id] ?: return null
        if (profile.id == uid || profile.leaderboardOptIn) {
            return profile
        }
        // Row is hidden by RLS
        return null
    }

    /**
     * SELECT * FROM public.profiles WHERE leaderboard_opt_in = TRUE:
     * Returns only opted-in profiles for authenticated users; empty for anon.
     */
    fun getLeaderboard(): List<CloudProfileDto> {
        if (currentUserId == null) return emptyList()
        return profiles.values.filter { it.leaderboardOptIn }
    }

    /**
     * INSERT / UPDATE on public.profiles:
     * Enforces policy: authenticated users can only insert or update their own row (id = auth.uid()).
     */
    fun upsertProfile(profile: CloudProfileDto) {
        val uid = currentUserId ?: throw SecurityException("RLS Violation: Anonymous users cannot write profiles")
        if (profile.id != uid) {
            throw SecurityException("RLS Violation: Cannot insert or update another user's profile row (uid=$uid, target=${profile.id})")
        }
        profiles[profile.id] = profile
    }

    /**
     * DELETE on public.profiles:
     * Enforces policy: user can only delete their own profile.
     */
    fun deleteProfile(id: String) {
        val uid = currentUserId ?: throw SecurityException("RLS Violation: Anonymous users cannot delete profiles")
        if (id != uid) {
            throw SecurityException("RLS Violation: Cannot delete another user's profile (uid=$uid, target=$id)")
        }
        profiles.remove(id)
    }

    /**
     * SELECT on public.usernames:
     * Enforces policy: owner can only read their own username row.
     */
    fun getUsername(userId: String): String? {
        val uid = currentUserId ?: throw SecurityException("RLS Violation: Anonymous users cannot read usernames")
        if (userId != uid) {
            throw SecurityException("RLS Violation: One user cannot read another's private username row")
        }
        return usernames[userId]
    }

    fun setUsername(userId: String, username: String) {
        val uid = currentUserId ?: throw SecurityException("RLS Violation: Anonymous users cannot set usernames")
        if (userId != uid) {
            throw SecurityException("RLS Violation: Cannot set username for another user")
        }
        usernames[userId] = username
    }

    /**
     * SELECT on public.party_members:
     * Enforces policy: only members of a party can read its members.
     */
    fun getPartyMembers(partyId: String): List<PartyMember> {
        val uid = currentUserId ?: throw SecurityException("RLS Violation: Anonymous users cannot read party members")
        val isMember = partyMembers.any { it.partyId == partyId && it.userId == uid && it.status == MemberStatus.ACTIVE }
        if (!isMember) {
            throw SecurityException("RLS Violation: User $uid is not an active member of party $partyId")
        }
        return partyMembers.filter { it.partyId == partyId }
    }

    fun addPartyMember(partyId: String, userId: String, role: PartyRole = PartyRole.MEMBER, status: MemberStatus = MemberStatus.ACTIVE) {
        partyMembers.add(
            PartyMember(
                partyId = partyId,
                userId = userId,
                username = usernames[userId] ?: "User_$userId",
                role = role,
                status = status,
                invitedAt = Instant.now(),
                joinedAt = Instant.now(),
            ),
        )
    }

    /**
     * INSERT on public.reports:
     * Reporter must match auth.uid(), and cannot report themselves.
     */
    fun reportUser(reportedUserId: String, reason: String) {
        val uid = currentUserId ?: throw SecurityException("RLS Violation: Anonymous users cannot file reports")
        if (uid == reportedUserId) {
            throw IllegalArgumentException("Cannot report yourself")
        }
        reports.add(ReportRecord(reporterId = uid, reportedUserId = reportedUserId, reason = reason))
    }
}
