package com.example.agendapersonal

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alarm_data")
data class AlarmData(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val hour: Int,
    val minute: Int,
    val ringtoneUri: String,
    val tanggal: String // Tambahkan kolom tanggal
)
