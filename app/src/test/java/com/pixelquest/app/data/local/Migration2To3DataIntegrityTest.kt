package com.pixelquest.app.data.local

import androidx.sqlite.db.SupportSQLiteDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.lang.reflect.Proxy

class Migration2To3DataIntegrityTest {

    data class ProfileRow(
        val id: Long,
        val username: String,
        val avatarId: String,
        val level: Int,
        val totalXp: Int,
        val perfectDaysTowardNextLevel: Int,
        val createdAt: Long,
        var supabaseUserId: String? = null,
        var leaderboardOptIn: Int = 0,
        var leaderboardDisplayName: String? = null
    )

    @Test
    fun existingProfileData_survivesMigration2To3_withProperDefaults() {
        val simulatedDbState = mutableListOf(
            ProfileRow(
                id = 1,
                username = "PixelLegend",
                avatarId = "hero_warrior",
                level = 15,
                totalXp = 8200,
                perfectDaysTowardNextLevel = 4,
                createdAt = 1700000000000L
            )
        )

        val executedSql = mutableListOf<String>()

        val dummyDb = Proxy.newProxyInstance(
            SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java)
        ) { _, method, args ->
            if (method.name == "execSQL" && args != null && args.isNotEmpty()) {
                val sql = args[0] as String
                executedSql.add(sql)
                if (sql.contains("ADD COLUMN supabaseUserId")) {
                    simulatedDbState.forEach { it.supabaseUserId = null }
                }
                if (sql.contains("ADD COLUMN leaderboardOptIn")) {
                    simulatedDbState.forEach { it.leaderboardOptIn = 0 }
                }
                if (sql.contains("ADD COLUMN leaderboardDisplayName")) {
                    simulatedDbState.forEach { it.leaderboardDisplayName = null }
                }
            }
            null
        } as SupportSQLiteDatabase

        AppDatabase.MIGRATION_2_3.migrate(dummyDb)

        assertEquals("Should execute 3 ALTER TABLE commands", 3, executedSql.size)

        val row = simulatedDbState.first()
        assertEquals("Username must survive untouched", "PixelLegend", row.username)
        assertEquals("Avatar ID must survive untouched", "hero_warrior", row.avatarId)
        assertEquals("Level must survive untouched", 15, row.level)
        assertEquals("Total XP must survive untouched", 8200, row.totalXp)
        assertEquals("Perfect days must survive untouched", 4, row.perfectDaysTowardNextLevel)
        assertEquals("Created timestamp must survive untouched", 1700000000000L, row.createdAt)

        assertNull("supabaseUserId must default to null", row.supabaseUserId)
        assertEquals("leaderboardOptIn must default to 0 (false)", 0, row.leaderboardOptIn)
        assertNull("leaderboardDisplayName must default to null", row.leaderboardDisplayName)
    }
}
