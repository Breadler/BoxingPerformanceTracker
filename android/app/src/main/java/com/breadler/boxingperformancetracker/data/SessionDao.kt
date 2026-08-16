package com.breadler.boxingperformancetracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

// Room data access object for the sessions table
@Dao
interface SessionDao {
    // Live list of all sessions, newest first
    @Query("SELECT * FROM sessions ORDER BY processedAtMs DESC")
    fun observeSessions(): Flow<List<SessionEntity>>

    // Single session by id
    @Query("SELECT * FROM sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getSession(sessionId: String): SessionEntity?

    // Insert or replace a session
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(session: SessionEntity)

    // Delete a session by id
    @Query("DELETE FROM sessions WHERE id = :sessionId")
    suspend fun delete(sessionId: String)
}
