package com.pickflow.android.feature.spotdetail

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.pickflow.android.common.designsystem.PickflowTheme
import com.pickflow.android.common.ui.LoadState
import com.pickflow.android.core.services.protocols.MySpotDetail
import com.pickflow.android.core.services.protocols.MySpotStatus
import com.pickflow.android.core.services.protocols.RejectionReason
import com.pickflow.android.core.services.protocols.SpotRejection
import com.pickflow.android.core.services.protocols.SpotSource
import com.pickflow.android.core.services.protocols.SpotTheme
import org.junit.Rule
import org.junit.Test

/** PV-41 공개 상태 전이별 상세 화면의 시각 회귀 계약. */
class SpotOpenSnapshotTest {

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5)

    @Test
    fun draft() = snapshot(detail(MySpotStatus.DRAFT))

    @Test
    fun pending() = snapshot(detail(MySpotStatus.PENDING))

    @Test
    fun rereviewPending() = snapshot(detail(MySpotStatus.RE_REVIEW_PENDING))

    @Test
    fun rejected() = snapshot(
        detail(
            status = MySpotStatus.REJECTED,
            rejection = SpotRejection(
                reason = RejectionReason.LOW_QUALITY,
                reasonLabel = "사진 상태 불량",
                guideMessage = "장소를 식별할 수 있는 사진이 필요해요.",
                detail = null,
                rejectedAt = "2026-08-06T10:00:00Z",
            ),
        ),
    )

    @Test
    fun publishedOwner() = snapshot(detail(MySpotStatus.PUBLISHED))

    @Test
    fun publishedCurated() = snapshot(
        detail(
            status = MySpotStatus.PUBLISHED,
            source = SpotSource.Curated("한국관광공사"),
        ),
    )

    @Test
    fun requestOpenSheet() = snapshot(
        detail = detail(MySpotStatus.DRAFT),
        initialSheet = SpotOpenSheet.REQUEST_OPEN,
    )

    private fun snapshot(
        detail: MySpotDetail,
        initialSheet: SpotOpenSheet? = null,
    ) {
        paparazzi.snapshot {
            PickflowTheme {
                SpotOpenDetailContent(
                    state = LoadState.Loaded(detail),
                    initialSheet = initialSheet,
                )
            }
        }
    }

    private fun detail(
        status: MySpotStatus,
        rejection: SpotRejection? = null,
        source: SpotSource = SpotSource.User,
    ) = MySpotDetail(
        id = 41L,
        name = "노을 공원",
        theme = SpotTheme.SUNSET,
        imageUrl = null,
        latitude = 37.0,
        longitude = 127.0,
        address = "서울특별시 마포구 하늘공원로",
        capturedDate = "2026-08-06",
        capturedTime = "19:20",
        comment = "노을이 예뻐요",
        status = status,
        rejection = rejection,
        recommendationCount = 7L,
        isRecommended = false,
        source = source,
        updatedAt = "2026-08-06T10:00:00Z",
    )
}
