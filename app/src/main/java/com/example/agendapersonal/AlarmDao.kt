package com.example.agendapersonal

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface AlarmDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlarm(alarmData: AlarmData)

    @Query("SELECT * FROM alarm_data ORDER BY id DESC LIMIT 1")
    suspend fun getLatestAlarm(): AlarmData?

    @Query("SELECT * FROM alarm_data ORDER BY id DESC")
    suspend fun getAllAlarms(): List<AlarmData>
}
