package com.example.jadwalharian

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface TaskDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task)

    @Delete
    suspend fun deleteTask(task: Task)

    @Query("SELECT * FROM tasks ORDER BY timestamp ASC")
    fun getAllTasks(): LiveData<List<Task>>

    // TAMBAHKAN FUNGSI BARU INI
    @Query("SELECT * FROM tasks WHERE timestamp >= :startOfDay AND timestamp < :endOfDay ORDER BY timestamp ASC")
    fun getTasksByDate(startOfDay: Long, endOfDay: Long): LiveData<List<Task>>

    // TAMBAHKAN FUNGSI BARU INI
    @Update
    suspend fun updateTask(task: Task)
}