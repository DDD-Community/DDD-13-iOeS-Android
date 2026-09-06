package com.pickflow.android.core.services.impl

import com.pickflow.android.core.network.api.MySpotApi
import com.pickflow.android.core.network.api.SpotApi
import com.pickflow.android.core.network.dto.myspot.CreateMySpotMetaRequest
import com.pickflow.android.core.network.dto.myspot.UpdateMySpotMetaRequest
import com.pickflow.android.core.network.mapper.toCreateMySpotResult
import com.pickflow.android.core.network.mapper.toMySpotDetail
import com.pickflow.android.core.network.mapper.toMySpotPage
import com.pickflow.android.core.network.mapper.toTransitionResult
import com.pickflow.android.core.network.mapper.toUnpublishResult
import com.pickflow.android.core.network.mapper.toUpdateResult
import com.pickflow.android.core.network.toMultipartPart
import com.pickflow.android.core.network.unwrap
import com.pickflow.android.core.network.unwrapVoid
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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * 전 오퍼레이션이 실서버다(2026-08-26 OpenAPI 실측으로 계약 확정 — `docs/PV-41/09-api-mapping.md`).
 *
 * 상세만 예외적으로 [SpotApi] 를 탄다. 나만의 스팟 전용 상세 엔드포인트는 서버에 **없고**
 * `GET /v1/spots/{spotId}` 가 `status`·`rejection`·`isCurated` 까지 내려주기 때문이다(B7).
 *
 * 낙관적 경합 감지는 넣지 않았다. 서버 응답 어느 스키마에도 `updatedAt` 이 없어 비교할
 * 토큰이 없다(B6). 전이 실패는 그대로 예외로 올라가 화면이 재시도 토스트를 띄운다.
 */
class DefaultMySpotService @Inject constructor(
    private val mySpotApi: MySpotApi,
    private val spotApi: SpotApi,
    private val json: Json,
) : MySpotService {
    override suspend fun list(page: Int, coordinates: Coordinates?): MySpotPage =
        mySpotApi.getMySpots(
            page = page,
            // 서버 검증: 위/경도 소수점 6자리까지 → truncate (7자리 GPS 시 400 방지).
            latitude = coordinates?.latitude?.toSixDecimal(),
            longitude = coordinates?.longitude?.toSixDecimal(),
        ).unwrap().toMySpotPage()

    override suspend fun detail(spotId: Long): MySpotDetail =
        spotApi.getSpotDetail(spotId).unwrap().toMySpotDetail()

    override suspend fun create(draft: SpotDraft, image: ImagePayload): CreateMySpotResult {
        // 이미지 part (공통 변환기 사용)
        val imagePart = image.toMultipartPart(IMAGE_PART)
        // 메타 part (JSON) — iOS `SpotService.registerSpot` 1:1: part 이름 `request`,
        // address 미포함, 위/경도 소수점 6자리 반올림(서버 검증).
        val meta = CreateMySpotMetaRequest(
            name = draft.name,
            theme = draft.theme.name,
            latitude = draft.latitude.toSixDecimal(),
            longitude = draft.longitude.toSixDecimal(),
            comment = draft.comment.takeIf { it.isNotBlank() },
            recordedDate = draft.capturedDate.takeIf { it.isNotBlank() },
            recordedTime = draft.capturedTime.takeIf { it.isNotBlank() },
        )

        return mySpotApi.createMySpot(image = imagePart, meta = meta.toMetaPart())
            .unwrap()
            .toCreateMySpotResult()
    }

    override suspend fun update(
        spotId: Long,
        draft: SpotDraft,
        replacementImage: ImagePayload?,
    ): MySpotUpdateResult {
        val meta = UpdateMySpotMetaRequest(
            name = draft.name,
            theme = draft.theme.name,
            latitude = draft.latitude.toSixDecimal(),
            longitude = draft.longitude.toSixDecimal(),
            comment = draft.comment.takeIf { it.isNotBlank() },
            recordedDate = draft.capturedDate.takeIf { it.isNotBlank() },
            recordedTime = draft.capturedTime.takeIf { it.isNotBlank() },
        )

        return mySpotApi.updateMySpot(
            spotId = spotId,
            meta = meta.toMetaPart(),
            // 미첨부면 서버가 기존 이미지를 유지한다.
            image = replacementImage?.toMultipartPart(IMAGE_PART),
        ).unwrap().toUpdateResult()
    }

    override suspend fun requestOpen(spotId: Long): MySpotTransitionResult =
        mySpotApi.requestOpen(spotId).unwrap().toTransitionResult()

    override suspend fun setReleased(spotId: Long, released: Boolean): Boolean {
        val response =
            if (released) mySpotApi.releaseSpot(spotId) else mySpotApi.unreleaseSpot(spotId)
        return response.unwrap().released
    }

    override suspend fun unpublish(spotId: Long): MySpotUnpublishResult =
        mySpotApi.cancelPublication(spotId).unwrap().toUnpublishResult()

    override suspend fun delete(spotId: Long) = mySpotApi.deleteMySpot(spotId).unwrapVoid()

    private inline fun <reified T> T.toMetaPart(): MultipartBody.Part =
        MultipartBody.Part.createFormData(
            name = META_PART,
            filename = null,
            body = json.encodeToString(this).toRequestBody(JSON_MEDIA),
        )

    private companion object {
        // iOS `SpotService.registerSpot` 과 동일한 part 이름 (BE 검증 완료 형태).
        const val IMAGE_PART = "image"
        const val META_PART = "request"
        val JSON_MEDIA = "application/json".toMediaType()
    }
}
