package com.pickflow.android.core.services.impl

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.pickflow.android.core.services.protocols.Coordinates
import com.pickflow.android.core.services.protocols.LocationService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * FusedLocationProviderClient 기반 실제 위치 서비스.
 * 권한이 없거나 위치를 얻지 못하면 null 을 반환한다.
 */
@Singleton
class DefaultLocationService @Inject constructor(
    @ApplicationContext private val context: Context,
) : LocationService {

    private val client by lazy { LocationServices.getFusedLocationProviderClient(context) }

    override suspend fun currentLocation(): Coordinates? {
        if (!hasLocationPermission()) return null
        val location = runCatching { requestCurrentLocation() }.getOrNull()
            ?: runCatching { requestLastLocation() }.getOrNull()
        return location?.let { Coordinates(it.latitude, it.longitude) }
    }

    @SuppressLint("MissingPermission")
    private suspend fun requestCurrentLocation(): Location? = suspendCancellableCoroutine { cont ->
        val cts = CancellationTokenSource()
        cont.invokeOnCancellation { cts.cancel() }
        client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
            .addOnSuccessListener { cont.resume(it) }
            .addOnFailureListener { cont.resumeWithException(it) }
    }

    @SuppressLint("MissingPermission")
    private suspend fun requestLastLocation(): Location? = suspendCancellableCoroutine { cont ->
        client.lastLocation
            .addOnSuccessListener { cont.resume(it) }
            .addOnFailureListener { cont.resumeWithException(it) }
    }

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
}
