package com.pickflow.android.core.services.impl

import com.pickflow.android.core.network.api.SpotApi
import com.pickflow.android.core.network.mapper.toSpotDetail
import com.pickflow.android.core.network.unwrap
import com.pickflow.android.core.services.protocols.Spot
import com.pickflow.android.core.services.protocols.SpotDetail
import com.pickflow.android.core.services.protocols.SpotDraft
import com.pickflow.android.core.services.protocols.SpotService
import com.pickflow.android.core.services.protocols.SpotTheme
import java.util.UUID
import javax.inject.Inject

class DefaultSpotService @Inject constructor(
    private val spotApi: SpotApi,
) : SpotService {
    override suspend fun spot(id: String): SpotDetail {
        val longId = id.toLongOrNull()
            ?: throw IllegalArgumentException("spotId는 정수여야 합니다: $id")
        return spotApi.getSpotDetail(longId).unwrap().toSpotDetail()
    }

    /**
     * TODO(Phase D-4): POST /v1/users/me/my-spots(MySpotService.create)로 이전. 그때 까지는
     * stub 동작 유지하여 기존 SpotRegistrationViewModel 흐름을 깨지 않는다.
     */
    override suspend fun register(draft: SpotDraft): Spot = Spot(
        id = "spot-${UUID.randomUUID()}",
        name = draft.name,
        theme = draft.theme.takeOrSunset(),
        latitude = draft.latitude,
        longitude = draft.longitude,
        imageUrl = draft.imageUrl,
        address = draft.address,
    )
}

private fun SpotTheme.takeOrSunset(): SpotTheme = this
