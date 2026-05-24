package com.pickflow.android.feature.login

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.Density
import com.pickflow.android.common.designsystem.PickflowTheme
import org.junit.Rule
import org.junit.Test

/**
 * iOS `LoginViewSnapshotTests` 12케이스 1:1 대응 Paparazzi 스냅샷.
 *
 * iOS LoginView는 `.preferredColorScheme(.dark)`로 항상 다크 렌더이므로
 * `light_forced` 케이스도 다크로 렌더된다. `alert_error`/`guest_requested`는
 * iOS에서 alert/flag만 바뀌어 스냅샷상 root와 동일하게 보인다.
 */
class LoginScreenSnapshotTest {

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = PHONE)

    @Test
    fun login_root_idle_dark() = snapshotLogin()

    @Test
    fun login_header_idle_dark() = snapshotLogin()

    @Test
    fun login_header_closable_dark() = snapshotLogin(isClosable = true)

    @Test
    fun login_center_content_idle_dark() = snapshotLogin()

    @Test
    fun login_cta_idle_dark() = snapshotLogin()

    @Test
    fun login_cta_kakao_loading_dark() = snapshotLogin(kakaoLoading = true)

    @Test
    fun login_cta_apple_loading_dark() = snapshotLogin(appleLoading = true)

    @Test
    fun login_alert_error_dark() = snapshotLogin()

    @Test
    fun login_guest_requested_dark() = snapshotLogin()

    @Test
    fun login_root_idle_light_forced_dark() = snapshotLogin()

    @Test
    fun login_root_accessibility_extra_large_dark() {
        paparazzi.unsafeUpdateConfig(PHONE.copy(fontScale = 2.0f))
        snapshotLogin()
    }

    @Test
    fun login_root_ipad_dark() {
        paparazzi.unsafeUpdateConfig(IPAD)
        snapshotLogin()
    }

    private fun snapshotLogin(
        kakaoLoading: Boolean = false,
        appleLoading: Boolean = false,
        isClosable: Boolean = false,
    ) {
        paparazzi.snapshot {
            PickflowTheme {
                LoginScreenContent(
                    kakaoLoading = kakaoLoading,
                    appleLoading = appleLoading,
                    isClosable = isClosable,
                )
            }
        }
    }

    companion object {
        /** iOS 390x844pt 대응. density 2.0(XHDPI) → 780x1688px. */
        private val PHONE = DeviceConfig.PIXEL_5.copy(
            screenWidth = 780,
            screenHeight = 1688,
            xdpi = 320,
            ydpi = 320,
            density = Density.XHIGH,
        )

        /** iOS iPad 834x1194pt 대응. */
        private val IPAD = PHONE.copy(
            screenWidth = 1668,
            screenHeight = 2388,
        )
    }
}
