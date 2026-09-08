package com.pixelquest.app.data.repository

import com.pixelquest.app.data.remote.SupabaseResult
import com.pixelquest.app.data.remote.model.CloudProfileDto
import com.pixelquest.app.data.remote.safeSupabaseCall
import io.github.jan.supabase.postgrest.Postgrest
import javax.inject.Inject
import javax.inject.Singleton

interface LeaderboardRepository {
    suspend fun getProfiles(): SupabaseResult<List<CloudProfileDto>>
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
}
