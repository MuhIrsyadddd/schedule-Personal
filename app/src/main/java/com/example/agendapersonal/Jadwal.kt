package com.example.agendapersonal

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "jadwal")
data class Jadwal(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val startDate: String,
    val timeStart: String,
    val timeEnd: String,
    val music: String,
    val description: String
)
