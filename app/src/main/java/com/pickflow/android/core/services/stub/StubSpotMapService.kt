package com.pickflow.android.core.services.stub

import com.pickflow.android.core.services.protocols.SpotMapMarker
import com.pickflow.android.core.services.protocols.SpotMapService
import com.pickflow.android.core.services.protocols.SpotTheme
import com.pickflow.android.core.services.protocols.ViewportBox
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StubSpotMapService @Inject constructor(
    private val backend: StubSpotBackend,
) : SpotMapService {
    override suspend fun fetchInViewport(box: ViewportBox, theme: SpotTheme?): List<SpotMapMarker> =
        backend.markers(box, theme)
}
