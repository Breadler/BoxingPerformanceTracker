package com.breadler.boxingperformancetracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

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

        fun getInstance(context: Context): SessionDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    SessionDatabase::class.java,
                    "stryko_sessions.db",
                )
                    // No real migrations written yet at this stage - a schema bump just
                    // recreates the table instead of crashing existing installs.
                    .fallbackToDestructiveMigration()
                    .build().also { instance = it }
            }
        }
    }
}
