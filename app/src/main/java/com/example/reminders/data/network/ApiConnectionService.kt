package com.example.reminders.data.network

import android.content.Context
import android.util.Log
import com.example.reminders.data.UserPreferencesRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Servicio centralizado para la configuración y creación de la conexión API.
 * Encapsula toda la lógica de construcción del cliente Retrofit y manejo de configuración.
 */
class ApiConnectionService(
    private val context: Context,
    private val userPreferencesRepository: UserPreferencesRepository
) {

    private var apiService: ApiService? = null

    /**
     * Obtiene la instancia del servicio API, construyéndola si es necesaria.
     * Reconstruye la instancia si la configuración del servidor ha cambiado.
     */
    fun getApiService(): ApiService {
        val currentUrl = buildBaseUrl()
        
        // Si el servicio ya existe y la URL no ha cambiado, reutilizarlo
        if (apiService != null && currentUrl == lastBaseUrl) {
            return apiService!!
        }
        
        lastBaseUrl = currentUrl
        apiService = createApiService(currentUrl)
        return apiService!!
    }

    /**
     * Construye la URL base según la configuración guardada.
     */
    private fun buildBaseUrl(): String {
        val address = runBlocking { userPreferencesRepository.serverAddress.first() }
        val port = runBlocking { userPreferencesRepository.serverPort.first() }
        val useHttps = runBlocking { userPreferencesRepository.useHttps.first() }

        val protocol = if (useHttps) "https" else "http"
        return if (address.isNotBlank()) {
            if (port.isNotBlank()) {
                "$protocol://$address:$port/"
            } else {
                "$protocol://$address/"
            }
        } else {
            "http://10.0.2.2:8080/"
        }
    }

    /**
     * Crea una nueva instancia del cliente API con la URL base proporcionada.
     */
    private fun createApiService(baseUrl: String): ApiService {
        Log.d("ApiConnectionService", "Creando cliente API con URL: $baseUrl")

        val loggingInterceptor = HttpLoggingInterceptor { message ->
            Log.d("OkHttp", message)
        }.apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val httpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(loggingInterceptor)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(ApiService::class.java)
    }

    /**
     * Invalida la instancia actual del servicio API.
     * Útil cuando se cambian las configuraciones del servidor.
     */
    fun invalidateApiService() {
        apiService = null
        lastBaseUrl = null
        Log.d("ApiConnectionService", "Instancia del API invalidada")
    }

    companion object {
        private var lastBaseUrl: String? = null
    }
}
