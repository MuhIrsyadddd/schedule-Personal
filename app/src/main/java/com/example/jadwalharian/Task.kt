package com.example.jadwalharian

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val timestamp: Long,
    val soundUri: String?,
    val status: String, // "Upcoming", "In Progress", "Done"
    val duration: String // "30 Minutes", "1.5 Hours"
)

