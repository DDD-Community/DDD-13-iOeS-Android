package com.pickflow.android.core.services.impl

import com.pickflow.android.core.services.protocols.Coordinates
import com.pickflow.android.core.services.protocols.LocationService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StubLocationService @Inject constructor() : LocationService {
    override suspend fun currentLocation(): Coordinates? =
        Coordinates(latitude = 37.5665, longitude = 126.9780)
}
