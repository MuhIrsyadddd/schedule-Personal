package com.example.jadwalharian

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0, // Dibuat var agar Room bisa set ID
    val title: String,
    val timestamp: Long,
    val soundUri: String? = null
) : Serializable