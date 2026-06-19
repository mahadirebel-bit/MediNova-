package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ReportDao {
    @Query("SELECT * FROM saved_reports ORDER BY dateMillis DESC")
    fun getAllReports(): Flow<List<SavedReport>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: SavedReport): Long

    @Query("DELETE FROM saved_reports WHERE id = :id")
    suspend fun deleteReportById(id: Long)

    @Query("DELETE FROM saved_reports")
    suspend fun clearAll()
}
