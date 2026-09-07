package com.pixelquest.app.data.local

import androidx.sqlite.db.SupportSQLiteDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Proxy

class Migration2To3Test {

    @Test
    fun migration2To3_hasCorrectVersionRange() {
        assertEquals(2, AppDatabase.MIGRATION_2_3.startVersion)
        assertEquals(3, AppDatabase.MIGRATION_2_3.endVersion)
    }

    @Test
    fun migration2To3_executesAlterTableQueries() {
        val executedSql = mutableListOf<String>()

        val dummyDb = Proxy.newProxyInstance(
            SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java)
        ) { _, method, args ->
            if (method.name == "execSQL" && args != null && args.isNotEmpty()) {
                executedSql.add(args[0] as String)
            }
            null
        } as SupportSQLiteDatabase

        AppDatabase.MIGRATION_2_3.migrate(dummyDb)

        assertEquals(3, executedSql.size)
        assertTrue(
            "Must alter user_profile table to add supabaseUserId column",
            executedSql.any { it.contains("ALTER TABLE user_profile ADD COLUMN supabaseUserId TEXT") }
        )
        assertTrue(
            "Must alter user_profile table to add leaderboardOptIn with default 0",
            executedSql.any { it.contains("ALTER TABLE user_profile ADD COLUMN leaderboardOptIn INTEGER NOT NULL DEFAULT 0") }
        )
        assertTrue(
            "Must alter user_profile table to add leaderboardDisplayName column",
            executedSql.any { it.contains("ALTER TABLE user_profile ADD COLUMN leaderboardDisplayName TEXT") }
        )
    }
}
