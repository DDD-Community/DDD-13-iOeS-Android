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
        // 서버 검증: 위/경도 소수점 6자리 한도.
        topLeftLat = box.topLeft.latitude.toSixDecimal(),
        topLeftLng = box.topLeft.longitude.toSixDecimal(),
        topRightLat = box.topRight.latitude.toSixDecimal(),
        topRightLng = box.topRight.longitude.toSixDecimal(),
        bottomLeftLat = box.bottomLeft.latitude.toSixDecimal(),
        bottomLeftLng = box.bottomLeft.longitude.toSixDecimal(),
        bottomRightLat = box.bottomRight.latitude.toSixDecimal(),
        bottomRightLng = box.bottomRight.longitude.toSixDecimal(),
        theme = theme?.name,
    ).unwrap().spots.map { it.toMapMarker() }
}
