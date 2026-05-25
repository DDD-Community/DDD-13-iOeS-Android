package com.pickflow.android.core.services.impl

import com.pickflow.android.core.network.api.MySpotApi
import com.pickflow.android.core.network.mapper.toMySpotPage
import com.pickflow.android.core.network.unwrap
import com.pickflow.android.core.services.protocols.Coordinates
import com.pickflow.android.core.services.protocols.MySpotPage
import com.pickflow.android.core.services.protocols.MySpotService
import javax.inject.Inject

class DefaultMySpotService @Inject constructor(
    private val mySpotApi: MySpotApi,
) : MySpotService {
    override suspend fun list(page: Int, coordinates: Coordinates?): MySpotPage =
        mySpotApi.getMySpots(
            page = page,
            latitude = coordinates?.latitude,
            longitude = coordinates?.longitude,
        ).unwrap().toMySpotPage()
}
