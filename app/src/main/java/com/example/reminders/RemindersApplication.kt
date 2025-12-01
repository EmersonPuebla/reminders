package com.example.reminders

import android.app.Application
import com.example.reminders.data.AppContainer
import com.example.reminders.data.AppDataContainer

class RemindersApplication : Application() {
    /**
     * El contenedor de dependencias de la aplicación.
     */
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        // Inicializa el contenedor de dependencias que proveerá los repositorios y servicios.
        container = AppDataContainer(this)
    }
}
