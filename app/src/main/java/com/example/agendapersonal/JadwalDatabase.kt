package com.example.agendapersonal

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Jadwal::class], version = 1, exportSchema = false)
abstract class JadwalDatabase : RoomDatabase() {
    abstract fun jadwalDao(): JadwalDao

    companion object {
        @Volatile
        private var INSTANCE: JadwalDatabase? = null

        fun getDatabase(context: Context): JadwalDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    JadwalDatabase::class.java,
                    "jadwal_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
