package com.pickflow.android.core.services.impl

import com.pickflow.android.core.services.protocols.Spot
import com.pickflow.android.core.services.protocols.SpotDraft
import com.pickflow.android.core.services.protocols.SpotService
import com.pickflow.android.core.services.protocols.SpotTheme
import java.util.UUID
import javax.inject.Inject

class StubSpotService @Inject constructor() : SpotService {
    override suspend fun spot(id: String): Spot = Spot(
        id = id,
        name = "Stub $id",
        theme = SpotTheme.CAFE,
        latitude = 37.5665,
        longitude = 126.9780,
        address = "Seoul, South Korea",
    )

    override suspend fun register(draft: SpotDraft): Spot = Spot(
        id = "spot-${UUID.randomUUID()}",
        name = draft.name,
        theme = draft.theme,
        latitude = draft.latitude,
        longitude = draft.longitude,
        imageUrl = draft.imageUrl,
        address = draft.address,
    )
}
