package com.pickflow.android.core.services.impl

import com.pickflow.android.core.network.api.SpotApi
import com.pickflow.android.core.network.mapper.toSpotDetail
import com.pickflow.android.core.network.mapper.toSpotPreview
import com.pickflow.android.core.network.unwrap
import com.pickflow.android.core.services.protocols.Coordinates
import com.pickflow.android.core.services.protocols.Spot
import com.pickflow.android.core.services.protocols.DevSettings
import com.pickflow.android.core.services.protocols.MySpotStatus
import com.pickflow.android.core.services.protocols.RejectionReason
import com.pickflow.android.core.services.protocols.SpotRejection
import com.pickflow.android.core.services.protocols.SpotDetail
import com.pickflow.android.core.services.protocols.SpotDraft
import com.pickflow.android.core.services.protocols.SpotPreview
import com.pickflow.android.core.services.protocols.SpotService
import com.pickflow.android.core.services.protocols.SpotTheme
import java.util.UUID
import javax.inject.Inject

class DefaultSpotService @Inject constructor(
    private val spotApi: SpotApi,
    private val devSettings: DevSettings,
) : SpotService {
    override suspend fun spot(id: String): SpotDetail {
        val longId = id.toLongOrNull()
            ?: throw IllegalArgumentException("spotId는 정수여야 합니다: $id")
        return spotApi.getSpotDetail(longId).unwrap().toSpotDetail().withForcedStatus()
    }

    /**
     * Dev Mode 의 상태 강제(디버그 전용). 어드민 검수 API 가 `USER_ADMIN` 전용이라
     * 일반 계정으로 반려·공개 상태를 만들 수 없어 상태별 UI 확인용으로 둔 임시 우회다.
     * 릴리스 빌드는 [DevSettings.forcedMySpotStatus] 가 항상 null 이라 그대로 통과한다.
     */
    private fun SpotDetail.withForcedStatus(): SpotDetail {
        val forced = devSettings.forcedMySpotStatus.value ?: return this
        if (!isMySpot) return this
        return copy(
            mySpotStatus = forced,
            // 반려 화면은 rejection 이 있어야 배너가 그려진다. 서버 값이 없으면 견본으로 채운다.
            rejection = rejection ?: SAMPLE_REJECTION.takeIf { forced == MySpotStatus.REJECTED },
        )
    }

    override suspend fun preview(id: String, coordinates: Coordinates?): SpotPreview {
        val longId = id.toLongOrNull()
            ?: throw IllegalArgumentException("spotId는 정수여야 합니다: $id")
        // 서버는 위/경도 소수점 6자리까지만 허용 → GPS 7자리 좌표를 6자리로 truncate.
        return spotApi.getSpotPreview(
            spotId = longId,
            latitude = coordinates?.latitude?.toSixDecimal(),
            longitude = coordinates?.longitude?.toSixDecimal(),
        ).unwrap().toSpotPreview()
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

/** Dev Mode 반려 상태 견본. 서버가 rejection 을 안 줄 때만 쓰인다. */
private val SAMPLE_REJECTION = SpotRejection(
    reason = RejectionReason.LOW_QUALITY,
    reasonLabel = "사진 품질이 기준에 못 미쳐요",
    guideMessage = "조금 더 밝은 시간대에 다시 촬영해서 신청해주세요.",
    detail = null,
    rejectedAt = "2026-08-27T10:00:00Z",
)
