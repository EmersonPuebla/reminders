package com.example.reminders

import android.app.Application
import com.example.reminders.data.AppContainer
import com.example.reminders.data.AppDataContainer

class RemindersApplication : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = AppDataContainer(this)
    }
}
