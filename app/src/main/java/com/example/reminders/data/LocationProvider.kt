package com.example.reminders.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class LocationProvider(private val context: Context) {

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Location? = suspendCoroutine { continuation ->
        val hasAccessFineLocationPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val hasAccessCoarseLocationPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasAccessFineLocationPermission && !hasAccessCoarseLocationPermission) {
            continuation.resume(null)
            return@suspendCoroutine
        }

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, CancellationTokenSource().token)
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    continuation.resume(location)
                } else {
                    // If current location is null, try last location
                    fusedLocationClient.lastLocation
                        .addOnSuccessListener { lastLocation: Location? ->
                            continuation.resume(lastLocation)
                        }
                        .addOnFailureListener { 
                            continuation.resume(null)
                        }
                }
            }
            .addOnFailureListener {
                // If getCurrentLocation fails, try last location
                fusedLocationClient.lastLocation
                    .addOnSuccessListener { lastLocation: Location? ->
                        continuation.resume(lastLocation)
                    }
                    .addOnFailureListener { 
                        continuation.resume(null)
                    }
            }
    }
}