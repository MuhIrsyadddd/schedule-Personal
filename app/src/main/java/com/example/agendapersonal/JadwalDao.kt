package com.example.agendapersonal

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface JadwalDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJadwal(jadwal: Jadwal)

    @Query("SELECT * FROM jadwal ORDER BY startDate ASC")
    suspend fun getAllJadwal(): List<Jadwal>
}
