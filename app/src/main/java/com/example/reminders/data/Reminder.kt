package com.example.reminders.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val description: String,
    val date: Long,
    val notify: Boolean,
    val notifyDate: Long? = null,
    val audioRecordings: Map<String, String> = emptyMap(),
    val attachments: Map<String, String> = emptyMap(),
    val sortOrder: Int = 0,
    val isSynced: Boolean = true,
    val isDeleted: Boolean = false,
    val lastModified: Long = System.currentTimeMillis()
)
