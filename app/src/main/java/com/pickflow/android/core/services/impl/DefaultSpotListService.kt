package com.pickflow.android.core.services.impl

import com.pickflow.android.core.network.api.SpotApi
import com.pickflow.android.core.network.mapper.toSpotPage
import com.pickflow.android.core.network.unwrap
import com.pickflow.android.core.services.protocols.Coordinates
import com.pickflow.android.core.services.protocols.SpotListService
import com.pickflow.android.core.services.protocols.SpotPage
import com.pickflow.android.core.services.protocols.SpotSort
import com.pickflow.android.core.services.protocols.SpotTheme
import javax.inject.Inject

class DefaultSpotListService @Inject constructor(
    private val spotApi: SpotApi,
) : SpotListService {
    override suspend fun fetch(
        theme: SpotTheme?,
        page: Int,
        coordinates: Coordinates?,
        sort: SpotSort,
    ): SpotPage = spotApi.getSpots(
        page = page,
        theme = theme?.name,
        // 서버 검증: 위/경도는 소수점 6자리까지 허용 → truncate.
        latitude = coordinates?.latitude?.toSixDecimal(),
        longitude = coordinates?.longitude?.toSixDecimal(),
        sort = sort.name,
    ).unwrap().toSpotPage()
}

/** 소수점 6자리로 잘라낸 좌표(서버 위/경도 검증 6자리 한도용). */
internal fun Double.toSixDecimal(): Double =
    kotlin.math.floor(this * 1_000_000.0) / 1_000_000.0

