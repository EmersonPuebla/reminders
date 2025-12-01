package com.example.reminders.data.network

import android.util.Log
import com.example.reminders.data.UserPreferencesRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Servicio centralizado para la configuración y creación de la conexión API.
 * Encapsula toda la lógica de construcción del cliente Retrofit y manejo de configuración.
 */
class ApiConnectionService(
    private val userPreferencesRepository: UserPreferencesRepository
) {

    val apiService: ApiService by lazy {
        createApiService()
    }

    val weatherService: WeatherService by lazy {
        createWeatherService()
    }

    private fun createWeatherService(): WeatherService {
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
            .baseUrl("https://api.openweathermap.org/data/2.5/")
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(WeatherService::class.java)
    }

    private fun createApiService(): ApiService {
        Log.d("ApiConnectionService", "Creando cliente API con interceptor dinámico de URL")

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
            .addInterceptor(UrlInterceptor(userPreferencesRepository))
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("http://placeholder.url/") // URL base de marcador de posición, será reemplazada por el interceptor
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(ApiService::class.java)
    }

    /**
     * Invalida la instancia actual del servicio API.
     * En la nueva implementación con interceptor, esto no es estrictamente necesario.
     */
    fun invalidateApiService() {
        // La URL se resuelve en cada petición gracias al interceptor.
        Log.d("ApiConnectionService", "La invalidación del servicio no es necesaria con la URL dinámica.")
    }
}

private class UrlInterceptor(
    private val userPreferencesRepository: UserPreferencesRepository
) : Interceptor {

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        val baseUrl = runBlocking {
            val address = userPreferencesRepository.serverAddress.first()
            val port = userPreferencesRepository.serverPort.first()
            val useHttps = userPreferencesRepository.useHttps.first()

            val protocol = if (useHttps) "https" else "http"
            if (address.isNotBlank()) {
                if (port.isNotBlank()) {
                    "$protocol://$address:$port/"
                } else {
                    "$protocol://$address/"
                }
            } else {
                "http://10.0.2.2:8080/" // Valor por defecto para el emulador
            }
        }.toHttpUrlOrNull()

        if (baseUrl != null) {
            val newUrl = originalRequest.url.newBuilder()
                .scheme(baseUrl.scheme)
                .host(baseUrl.host)
                .port(baseUrl.port)
                .build()

            val newRequest = originalRequest.newBuilder()
                .url(newUrl)
                .build()

            return chain.proceed(newRequest)
        }

        return chain.proceed(originalRequest)
    }
}
