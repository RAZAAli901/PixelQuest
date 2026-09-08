package com.pixelquest.app.data.repository

import com.pixelquest.app.data.remote.SupabaseResult
import com.pixelquest.app.data.remote.model.CloudProfileDto
import com.pixelquest.app.data.remote.safeSupabaseCall
import io.github.jan.supabase.postgrest.Postgrest
import javax.inject.Inject
import javax.inject.Singleton

interface LeaderboardRepository {
    suspend fun getProfiles(): SupabaseResult<List<CloudProfileDto>>
    suspend fun getTopByStreak(limit: Long = 20): SupabaseResult<List<CloudProfileDto>>
    suspend fun getTopByLevel(limit: Long = 20): SupabaseResult<List<CloudProfileDto>>
}

@Singleton
open class LeaderboardRepositoryImpl @Inject constructor(
    private val postgrest: Postgrest
) : LeaderboardRepository {

    override suspend fun getProfiles(): SupabaseResult<List<CloudProfileDto>> {
        return safeSupabaseCall {
            postgrest["profiles"].select {
                filter {
                    eq("leaderboard_opt_in", true)
                }
            }.decodeList<CloudProfileDto>()
        }
    }

    override suspend fun getTopByStreak(limit: Long): SupabaseResult<List<CloudProfileDto>> {
        return safeSupabaseCall {
            postgrest["profiles"].select {
                filter {
                    eq("leaderboard_opt_in", true)
                }
                order("current_streak", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                limit(limit)
            }.decodeList<CloudProfileDto>()
        }
    }

    override suspend fun getTopByLevel(limit: Long): SupabaseResult<List<CloudProfileDto>> {
        return safeSupabaseCall {
            postgrest["profiles"].select {
                filter {
                    eq("leaderboard_opt_in", true)
                }
                order("level", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                order("total_xp", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                limit(limit)
            }.decodeList<CloudProfileDto>()
        }
    }
}
