package com.example.reminders.data

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class Converters {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    @TypeConverter
    fun fromTimestamp(value: Long?): String? {
        return value?.let { dateFormat.format(Date(it)) }
    }

    @TypeConverter
    fun dateToTimestamp(date: String?): Long? {
        return date?.let { dateFormat.parse(it)?.time }
    }

    @TypeConverter
    fun fromString(value: String): Map<String, String> {
        val mapType = object : TypeToken<Map<String, String>>() {}.type
        return Gson().fromJson(value, mapType)
    }

    @TypeConverter
    fun fromMap(map: Map<String, String>): String {
        val gson = Gson()
        return gson.toJson(map)
    }
}
