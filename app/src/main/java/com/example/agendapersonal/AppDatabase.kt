package com.example.agendapersonal

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [AlarmData::class], version = 2) // Ubah versi dari 1 ke 2
abstract class AppDatabase : RoomDatabase() {
    abstract fun alarmDao(): AlarmDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "alarm_database"
                ).fallbackToDestructiveMigration() // Tambahkan ini agar database diperbarui
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
