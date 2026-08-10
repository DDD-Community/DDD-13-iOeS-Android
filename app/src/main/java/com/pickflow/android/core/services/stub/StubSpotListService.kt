package com.pickflow.android.core.services.stub

import com.pickflow.android.core.services.protocols.Coordinates
import com.pickflow.android.core.services.protocols.SpotListService
import com.pickflow.android.core.services.protocols.SpotPage
import com.pickflow.android.core.services.protocols.SpotSort
import com.pickflow.android.core.services.protocols.SpotTheme
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StubSpotListService @Inject constructor(
    private val backend: StubSpotBackend,
) : SpotListService {
    override suspend fun fetch(
        theme: SpotTheme?,
        page: Int,
        coordinates: Coordinates?,
        sort: SpotSort,
    ): SpotPage = backend.publicSpots(theme, page, coordinates, sort)
}
