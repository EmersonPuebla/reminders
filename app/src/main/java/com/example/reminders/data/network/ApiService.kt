package com.example.reminders.data.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface ApiService {

    @GET("api/v1/reminders")
    suspend fun getAllReminders(): ReminderApiResponse<List<ReminderDto>>

    @GET("api/v1/reminders/{id}")
    suspend fun getReminderById(@Path("id") id: Int): ReminderApiResponse<ReminderDto>

    @POST("api/v1/reminders")
    suspend fun createReminder(@Body reminder: ReminderDto): ReminderApiResponse<ReminderDto>

    @PUT("api/v1/reminders/{id}")
    suspend fun updateReminder(@Path("id") id: Int, @Body reminder: ReminderDto): ReminderApiResponse<ReminderDto>

    @DELETE("api/v1/reminders/{id}")
    suspend fun deleteReminder(@Path("id") id: Int): ReminderApiResponse<Unit>

    @GET("api/v1/reminders")
    suspend fun testConnection(): Response<Unit> // Nuevo método
}
