package com.abosalehg.khizana.data.db

import androidx.room.withTransaction

/**
 * Seam over Room's `withTransaction`. Multi-step writes go through this so a
 * crash or cancellation mid-way can never leave half a rescan, half a restore
 * or a half-rewritten shelf order behind — and so repositories stay unit
 * testable on the JVM with a pass-through implementation.
 */
interface TransactionRunner {
    suspend operator fun <R> invoke(block: suspend () -> R): R
}

internal class RoomTransactionRunner(
    private val database: KhizanaDatabase
) : TransactionRunner {
    override suspend fun <R> invoke(block: suspend () -> R): R =
        database.withTransaction { block() }
}
