package com.pickflow.android.core.services.impl

import com.pickflow.android.core.network.api.MySpotApi
import com.pickflow.android.core.network.dto.myspot.CreateMySpotMetaRequest
import com.pickflow.android.core.network.mapper.toCreateMySpotResult
import com.pickflow.android.core.network.mapper.toMySpotPage
import com.pickflow.android.core.network.toMultipartPart
import com.pickflow.android.core.network.unwrap
import com.pickflow.android.core.services.protocols.Coordinates
import com.pickflow.android.core.services.protocols.CreateMySpotResult
import com.pickflow.android.core.services.protocols.ImagePayload
import com.pickflow.android.core.services.protocols.MySpotPage
import com.pickflow.android.core.services.protocols.MySpotService
import com.pickflow.android.core.services.protocols.SpotDraft
import javax.inject.Inject
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class DefaultMySpotService @Inject constructor(
    private val mySpotApi: MySpotApi,
    private val json: Json,
) : MySpotService {
    override suspend fun list(page: Int, coordinates: Coordinates?): MySpotPage =
        mySpotApi.getMySpots(
            page = page,
            // 서버 검증: 위/경도 소수점 6자리까지 → truncate (7자리 GPS 시 400 방지).
            latitude = coordinates?.latitude?.toSixDecimal(),
            longitude = coordinates?.longitude?.toSixDecimal(),
        ).unwrap().toMySpotPage()

    override suspend fun create(draft: SpotDraft, image: ImagePayload): CreateMySpotResult {
        // 이미지 part (공통 변환기 사용)
        val imagePart = image.toMultipartPart(IMAGE_PART)
        // 메타 part (JSON)
        val meta = CreateMySpotMetaRequest(
            name = draft.name,
            theme = draft.theme.name,
            latitude = draft.latitude,
            longitude = draft.longitude,
            address = draft.address,
            comment = draft.comment.takeIf { it.isNotBlank() },
            recordedDate = draft.capturedDate.takeIf { it.isNotBlank() },
            recordedTime = draft.capturedTime.takeIf { it.isNotBlank() },
        )
        val metaBody = json.encodeToString(meta).toRequestBody(JSON_MEDIA)
        val metaPart = MultipartBody.Part.createFormData(
            name = META_PART, filename = null, body = metaBody,
        )

        return mySpotApi.createMySpot(image = imagePart, meta = metaPart)
            .unwrap()
            .toCreateMySpotResult()
    }

    private companion object {
        // TODO(BE confirm): part 이름 검증 후 확정.
        const val IMAGE_PART = "image"
        const val META_PART = "meta"
        val JSON_MEDIA = "application/json".toMediaType()
    }
}
