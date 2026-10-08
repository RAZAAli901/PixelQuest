package com.pixelquest.app.data.local

import androidx.room.withTransaction
import javax.inject.Inject
import javax.inject.Singleton

/** Runs a group of repository writes as one database transaction: all of them are saved, or none. */
interface TransactionRunner {
    suspend fun <T> inTransaction(block: suspend () -> T): T
}

@Singleton
class RoomTransactionRunner @Inject constructor(private val db: AppDatabase) : TransactionRunner {
    override suspend fun <T> inTransaction(block: suspend () -> T): T = db.withTransaction(block)
}
