package com.pickflow.android.feature.spotregistration

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
import com.pickflow.android.core.services.protocols.SpotTheme
import org.junit.Rule
import org.junit.Test

/**
 * PV-59 등록 폼 "사진 카테고리" 칩 스냅샷 — `docs/PV-59/ui-test-cases.md` PV59-REG-01/02.
 *
 * 등록 폼은 로그인이 필요해 **비회원 상태의 에뮬레이터로는 진입할 수 없다.**
 * 따라서 REG 시나리오의 UI 증적은 에뮬레이터 스크린샷 대신 이 스냅샷이 담당한다
 * (동작 검증은 `SpotRegistrationScreenUiTest` 가 Robolectric 으로 수행).
 */
class SpotRegistrationThemeChipSnapshotTest {

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = device(390, 64))

    /** PV59-REG-01 — 칩 4종이 햇살→윤슬→노을→야경 순으로, 초기 미선택 상태. */
    @Test
    fun registration_theme_chips_unselected_dark() = chips(null)

    /** PV59-REG-02 — 햇살 단독 선택. */
    @Test
    fun registration_theme_chips_sunlight_selected_dark() = chips(SpotTheme.SUNLIGHT)

    /** PV59-REG-02 — 야경 단독 선택. 햇살이 함께 켜져 있으면 단독 선택 회귀다. */
    @Test
    fun registration_theme_chips_night_selected_dark() = chips(SpotTheme.NIGHT)

    private fun chips(selected: SpotTheme?) {
        paparazzi.snapshot {
            PickflowTheme {
                Box(
                    modifier = Modifier.fillMaxSize().background(PickflowColors.gray95),
                    contentAlignment = Alignment.Center,
                ) {
                    ThemeChipGroup(selected = selected, onToggle = {})
                }
            }
        }
    }

    private companion object {
        fun device(wDp: Int, hDp: Int): DeviceConfig = DeviceConfig.PIXEL_5.copy(
            screenWidth = wDp * 2,
            screenHeight = hDp * 2,
            xdpi = 320,
            ydpi = 320,
            density = Density.XHIGH,
            orientation = if (wDp > hDp) ScreenOrientation.LANDSCAPE else ScreenOrientation.PORTRAIT,
        )
    }
}
