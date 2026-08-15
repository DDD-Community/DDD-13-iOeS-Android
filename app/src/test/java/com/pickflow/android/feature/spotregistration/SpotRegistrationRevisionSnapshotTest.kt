package com.pickflow.android.feature.spotregistration

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.Density
import com.pickflow.android.common.designsystem.PickflowTheme
import com.pickflow.android.common.ui.LoadState
import com.pickflow.android.core.services.protocols.AddressSuggestion
import com.pickflow.android.core.services.protocols.MySpotDetail
import com.pickflow.android.core.services.protocols.MySpotStatus
import com.pickflow.android.core.services.protocols.RejectionReason
import com.pickflow.android.core.services.protocols.SpotRejection
import com.pickflow.android.core.services.protocols.SpotSource
import com.pickflow.android.core.services.protocols.SpotTheme
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Rule
import org.junit.Test

class SpotRegistrationRevisionSnapshotTest {

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = device())

    @Test
    fun revision_loaded_dark() {
        snapshot(showResubmitSheet = false)
    }

    @Test
    fun revision_resubmit_sheet_dark() {
        snapshot(showResubmitSheet = true)
    }

    private fun snapshot(showResubmitSheet: Boolean) {
        paparazzi.snapshot {
            PickflowTheme {
                SpotRegistrationContent(
                    mode = SpotRegistrationMode.REVISE,
                    revisionLoadState = LoadState.Loaded(detail),
                    selectedAddress = AddressSuggestion(
                        name = detail.name,
                        fullAddress = detail.address,
                        latitude = detail.latitude,
                        longitude = detail.longitude,
                    ),
                    distanceText = "1.2km",
                    spotName = detail.name,
                    theme = detail.theme,
                    capturedDate = LocalDate.parse(detail.capturedDate),
                    capturedTime = LocalTime.parse(detail.capturedTime),
                    comment = detail.comment,
                    selectedImageUri = null,
                    hasReplacementImage = false,
                    existingImageUrl = detail.imageUrl,
                    submission = LoadState.Idle,
                    isRegisterEnabled = true,
                    showResubmitSheet = showResubmitSheet,
                )
            }
        }
    }

    private val detail = MySpotDetail(
        id = 41L,
        name = "기존 노을 스팟",
        theme = SpotTheme.YUNSEUL,
        imageUrl = "https://cdn.example.com/41.jpg",
        latitude = 37.55,
        longitude = 127.01,
        address = "서울특별시 용산구 노을길 41",
        capturedDate = "2026-05-20",
        capturedTime = "19:40",
        comment = "기존 코멘트",
        status = MySpotStatus.REJECTED,
        rejection = SpotRejection(
            reason = RejectionReason.LOW_QUALITY,
            reasonLabel = "사진 상태 불량",
            guideMessage = "사진이 흐려요",
            detail = null,
            rejectedAt = "2026-08-06T10:00:00Z",
        ),
        recommendationCount = 3L,
        isRecommended = false,
        source = SpotSource.User,
        updatedAt = "2026-08-06T10:00:00Z",
    )

    private companion object {
        fun device(): DeviceConfig = DeviceConfig.PIXEL_5.copy(
            screenWidth = 393 * 2,
            screenHeight = 852 * 2,
            xdpi = 320,
            ydpi = 320,
            density = Density.XHIGH,
        )
    }
}
