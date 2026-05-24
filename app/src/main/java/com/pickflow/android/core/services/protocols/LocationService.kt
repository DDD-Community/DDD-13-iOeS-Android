package com.pickflow.android.core.services.protocols

data class Coordinates(val latitude: Double, val longitude: Double)

interface LocationService {
    suspend fun currentLocation(): Coordinates?
}
