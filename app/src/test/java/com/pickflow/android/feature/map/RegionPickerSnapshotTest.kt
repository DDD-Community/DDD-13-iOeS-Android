package com.pickflow.android.feature.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.Density
import com.android.resources.ScreenOrientation
import com.pickflow.android.common.designsystem.PickflowColors
import com.pickflow.android.common.designsystem.PickflowTheme
import com.pickflow.android.core.services.protocols.Region
import com.pickflow.android.feature.map.components.RegionPickerContent
import org.junit.Rule
import org.junit.Test

/**
 * PV-65 지역 선택 바텀시트 스냅샷 (Phase C).
 *
 * 대상은 모달 창을 벗겨낸 `RegionPickerContent` 다. 확인 포인트는 "현재 적용 중인 지역이
 * 선택된 상태로 뜬다" — 즉 `applied` 만 sunsetOrange 테두리·체크를 갖는다.
 * 시트는 정적 다크 토큰(gray95/gray90)만 쓰므로 dark 이름 하나만 record 한다.
 */
class RegionPickerSnapshotTest {

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = device(390, 360))

    /** 초기값(서울) 적용 상태 — 서울 행에만 테두리와 체크가 붙는다. */
    @Test
    fun regionpicker_seoul_applied_dark() = sheet(Region.Seoul)

    /** 대전 적용 상태 — 선택 표시가 대전 행으로 넘어간다. */
    @Test
    fun regionpicker_daejeon_applied_dark() = sheet(Region.Daejeon)

    /** 좁은 기기(320dp) — 취소/적용하기 두 버튼 라벨이 한 줄로 남는지 확인. */
    @Test
    fun regionpicker_narrow_320dp_dark() {
        paparazzi.unsafeUpdateConfig(device(320, 360))
        sheet(Region.Seoul)
    }

    private fun sheet(applied: Region) {
        paparazzi.snapshot {
            PickflowTheme {
                Box(
                    modifier = Modifier.fillMaxSize().background(PickflowColors.gray95),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    RegionPickerContent(applied = applied, onApply = {}, onCancel = {})
                }
            }
        }
    }

    private companion object {
        fun device(wDp: Int, hDp: Int): DeviceConfig =
            DeviceConfig.PIXEL_5.copy(
                screenWidth = wDp * 2,
                screenHeight = hDp * 2,
                xdpi = 320,
                ydpi = 320,
                density = Density.XHIGH,
                orientation = if (wDp > hDp) ScreenOrientation.LANDSCAPE else ScreenOrientation.PORTRAIT,
            )
    }
}
