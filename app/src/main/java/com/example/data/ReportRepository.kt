package com.example.data

import kotlinx.coroutines.flow.Flow

class ReportRepository(
    private val reportDao: ReportDao,
    private val reminderDao: ReminderDao
) {
    val allReports: Flow<List<SavedReport>> = reportDao.getAllReports()
    val allReminders: Flow<List<MedicineReminder>> = reminderDao.getAllReminders()

    suspend fun insert(report: SavedReport): Long {
        return reportDao.insertReport(report)
    }

    suspend fun deleteById(id: Long) {
        reportDao.deleteReportById(id)
    }

    suspend fun clearAll() {
        reportDao.clearAll()
    }

    suspend fun insertReminder(reminder: MedicineReminder): Long {
        return reminderDao.insertReminder(reminder)
    }

    suspend fun updateReminder(reminder: MedicineReminder) {
        reminderDao.updateReminder(reminder)
    }

    suspend fun deleteReminderById(id: Int) {
        reminderDao.deleteReminderById(id)
    }

    suspend fun clearAllReminders() {
        reminderDao.clearAllReminders()
    }
}
