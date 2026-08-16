package com.breadler.boxingperformancetracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// Room database holding the sessions table
@Database(
    entities = [SessionEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class SessionDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao

    companion object {
        @Volatile
        private var instance: SessionDatabase? = null

        // Singleton database instance
        fun getInstance(context: Context): SessionDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    SessionDatabase::class.java,
                    "stryko_sessions.db",
                )
                    // Destructive migration (no migrations written yet)
                    .fallbackToDestructiveMigration()
                    .build().also { instance = it }
            }
        }
    }
}
