package com.pickflow.android.core.services.stub

import com.pickflow.android.core.services.protocols.Coordinates
import com.pickflow.android.core.services.protocols.CreateMySpotResult
import com.pickflow.android.core.services.protocols.ImagePayload
import com.pickflow.android.core.services.protocols.MySpotDetail
import com.pickflow.android.core.services.protocols.MySpotPage
import com.pickflow.android.core.services.protocols.MySpotService
import com.pickflow.android.core.services.protocols.MySpotTransitionResult
import com.pickflow.android.core.services.protocols.MySpotUnpublishResult
import com.pickflow.android.core.services.protocols.MySpotUpdateResult
import com.pickflow.android.core.services.protocols.SpotDraft
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StubMySpotService @Inject constructor(
    private val backend: StubSpotBackend,
) : MySpotService {
    override suspend fun list(page: Int, coordinates: Coordinates?): MySpotPage =
        backend.mySpots(page, coordinates)

    override suspend fun detail(spotId: Long): MySpotDetail = backend.detail(spotId)

    override suspend fun create(draft: SpotDraft, image: ImagePayload): CreateMySpotResult =
        backend.create(draft, image)

    override suspend fun requestOpen(spotId: Long): MySpotTransitionResult =
        backend.requestOpen(spotId)

    override suspend fun unpublish(spotId: Long): MySpotUnpublishResult = backend.unpublish(spotId)

    override suspend fun update(
        spotId: Long,
        draft: SpotDraft,
        replacementImage: ImagePayload?,
    ): MySpotUpdateResult = backend.update(spotId, draft, replacementImage)

    override suspend fun delete(spotId: Long) = backend.delete(spotId)
}
