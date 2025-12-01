package com.example.reminders.data.network

data class ReminderApiResponse<T>(
    val success: Boolean,
    val message: String,
    val data: T? = null
)

data class ReminderDto(
    val id: Int,
    val title: String,
    val description: String,
    val notify: Boolean,
    val notifyDate: String? = null,
    val date: String,
    val voiceNotes: List<VoiceNoteDto> = emptyList(),
    val attachments: List<AttachmentDto> = emptyList(),
    val lastModified: Long
)

data class VoiceNoteDto(
    val name: String,
    val data: String? = null
)

data class AttachmentDto(
    val name: String,
    val data: String? = null
)
