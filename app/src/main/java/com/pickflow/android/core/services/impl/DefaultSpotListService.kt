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
        latitude = coordinates?.latitude,
        longitude = coordinates?.longitude,
        sort = sort.name,
    ).unwrap().toSpotPage()
}
