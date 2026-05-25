package com.pickflow.android.core.services.impl

import com.pickflow.android.core.network.api.SpotApi
import com.pickflow.android.core.network.mapper.toMapMarker
import com.pickflow.android.core.network.unwrap
import com.pickflow.android.core.services.protocols.SpotMapMarker
import com.pickflow.android.core.services.protocols.SpotMapService
import com.pickflow.android.core.services.protocols.SpotTheme
import com.pickflow.android.core.services.protocols.ViewportBox
import javax.inject.Inject

class DefaultSpotMapService @Inject constructor(
    private val spotApi: SpotApi,
) : SpotMapService {
    override suspend fun fetchInViewport(
        box: ViewportBox,
        theme: SpotTheme?,
    ): List<SpotMapMarker> = spotApi.getSpotsInViewport(
        topLeftLat = box.topLeft.latitude,
        topLeftLng = box.topLeft.longitude,
        topRightLat = box.topRight.latitude,
        topRightLng = box.topRight.longitude,
        bottomLeftLat = box.bottomLeft.latitude,
        bottomLeftLng = box.bottomLeft.longitude,
        bottomRightLat = box.bottomRight.latitude,
        bottomRightLng = box.bottomRight.longitude,
        theme = theme?.name,
    ).unwrap().spots.map { it.toMapMarker() }
}
