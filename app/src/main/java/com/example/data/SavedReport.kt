package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_reports")
data class SavedReport(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val dateMillis: Long = System.currentTimeMillis(),
    val reportType: String,
    val rawJson: String
)
