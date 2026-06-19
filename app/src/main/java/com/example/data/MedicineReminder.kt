package com.example.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "medicine_reminders")
data class MedicineReminder(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val medicineName: String,
    val dosage: String,
    val frequency: String,
    val time: String,
    val instructions: String,
    val isActive: Boolean = true,
    val creationTime: Long = System.currentTimeMillis()
)

@Dao
interface ReminderDao {
    @Query("SELECT * FROM medicine_reminders ORDER BY creationTime DESC")
    fun getAllReminders(): Flow<List<MedicineReminder>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: MedicineReminder): Long

    @Update
    suspend fun updateReminder(reminder: MedicineReminder)

    @Query("DELETE FROM medicine_reminders WHERE id = :id")
    suspend fun deleteReminderById(id: Int)

    @Query("DELETE FROM medicine_reminders")
    suspend fun clearAllReminders()
}
