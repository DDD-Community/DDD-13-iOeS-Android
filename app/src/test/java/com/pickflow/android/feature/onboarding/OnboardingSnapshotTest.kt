package com.pickflow.android.feature.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.Density
import com.android.resources.ScreenOrientation
import com.pickflow.android.common.designsystem.PickflowTheme
import com.pickflow.android.feature.onboarding.components.OnboardingIllustration
import com.pickflow.android.feature.onboarding.components.OnboardingPageIndicator
import com.pickflow.android.feature.onboarding.components.OnboardingPalette
import com.pickflow.android.feature.onboarding.components.OnboardingPanel
import com.pickflow.android.feature.onboarding.components.OnboardingPrimaryButton
import com.pickflow.android.feature.onboarding.model.defaultOnboardingPages
import org.junit.Rule
import org.junit.Test

/**
 * iOS `OnboardingSnapshotTests` 30케이스 1:1 대응 Paparazzi 스냅샷.
 *
 * iOS `OnboardingPalette`는 고정 색(gray95/gray0/...)을 쓰므로 light/dark 렌더가
 * 동일하다 — Android도 동일하게 같은 컨텐츠를 양쪽 이름으로 record한다.
 */
class OnboardingSnapshotTest {

    private val pages = defaultOnboardingPages

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = device(390, 844))

    // MARK: - Full screen (page 0~3, light/dark)

    @Test fun onboarding_screen_page0_light() = screen(0)
    @Test fun onboarding_screen_page0_dark() = screen(0)
    @Test fun onboarding_screen_page1_light() = screen(1)
    @Test fun onboarding_screen_page1_dark() = screen(1)
    @Test fun onboarding_screen_page2_light() = screen(2)
    @Test fun onboarding_screen_page2_dark() = screen(2)
    @Test fun onboarding_screen_page3_light() = screen(3)
    @Test fun onboarding_screen_page3_dark() = screen(3)

    // MARK: - Accessibility (DynamicType extra large)

    @Test fun onboarding_screen_page0_a11y() = screen(0, fontScale = 1.8f)
    @Test fun onboarding_screen_page3_a11y() = screen(3, fontScale = 1.8f)

    // MARK: - Production OnboardingView regression

    @Test fun onboardingView_page0_light() = screen(0)
    @Test fun onboardingView_page3_light() = screen(3)

    // MARK: - Page indicator

    @Test fun onboarding_indicator_page0_light() = indicator(0)
    @Test fun onboarding_indicator_page1_light() = indicator(1)
    @Test fun onboarding_indicator_page2_light() = indicator(2)
    @Test fun onboarding_indicator_page3_light() = indicator(3)

    // MARK: - Bottom panel

    @Test fun onboarding_panel_page0_light() = panel(0)
    @Test fun onboarding_panel_page0_dark() = panel(0)
    @Test fun onboarding_panel_page3_light() = panel(3)
    @Test fun onboarding_panel_page3_dark() = panel(3)

    // MARK: - CTA

    @Test fun onboarding_cta_light() = cta()
    @Test fun onboarding_cta_dark() = cta()

    // MARK: - Illustration component

    @Test fun onboarding_illustration_step0_light() = illustration(0)
    @Test fun onboarding_illustration_step0_dark() = illustration(0)
    @Test fun onboarding_illustration_step1_light() = illustration(1)
    @Test fun onboarding_illustration_step1_dark() = illustration(1)
    @Test fun onboarding_illustration_step2_light() = illustration(2)
    @Test fun onboarding_illustration_step2_dark() = illustration(2)
    @Test fun onboarding_illustration_step3_light() = illustration(3)
    @Test fun onboarding_illustration_step3_dark() = illustration(3)

    // MARK: - Renderers

    private fun screen(index: Int, fontScale: Float = 1f) {
        paparazzi.unsafeUpdateConfig(device(390, 844, fontScale))
        snapshot {
            OnboardingScreenContent(
                page = pages[index],
                currentIndex = index,
                pageCount = pages.size,
            )
        }
    }

    private fun indicator(index: Int) {
        paparazzi.unsafeUpdateConfig(device(160, 60))
        snapshot {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(OnboardingPalette.panelBackground),
                contentAlignment = Alignment.Center,
            ) {
                OnboardingPageIndicator(count = pages.size, currentIndex = index)
            }
        }
    }

    private fun panel(index: Int) {
        paparazzi.unsafeUpdateConfig(device(393, 320))
        snapshot {
            OnboardingPanel(
                page = pages[index],
                currentIndex = index,
                pageCount = pages.size,
                onPrimaryTap = {},
            )
        }
    }

    private fun cta() {
        paparazzi.unsafeUpdateConfig(device(393, 96))
        snapshot {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(OnboardingPalette.panelBackground),
                contentAlignment = Alignment.Center,
            ) {
                OnboardingPrimaryButton(
                    title = "시작하기",
                    onClick = {},
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
        }
    }

    private fun illustration(index: Int) {
        paparazzi.unsafeUpdateConfig(device(393, 600))
        snapshot {
            OnboardingIllustration(page = pages[index], modifier = Modifier.fillMaxSize())
        }
    }

    private fun snapshot(content: @Composable () -> Unit) {
        paparazzi.snapshot { PickflowTheme { content() } }
    }

    private companion object {
        /**
         * density 2.0(XHDPI)로 dp 좌표를 px로 환산한 DeviceConfig.
         * w>h인 가로형 캔버스는 orientation=LANDSCAPE로 지정해야 layoutlib가
         * 세로로 강제 회전시키지 않는다.
         */
        fun device(wDp: Int, hDp: Int, fontScale: Float = 1f): DeviceConfig =
            DeviceConfig.PIXEL_5.copy(
                screenWidth = wDp * 2,
                screenHeight = hDp * 2,
                xdpi = 320,
                ydpi = 320,
                density = Density.XHIGH,
                fontScale = fontScale,
                orientation = if (wDp > hDp) ScreenOrientation.LANDSCAPE else ScreenOrientation.PORTRAIT,
            )
    }
}
