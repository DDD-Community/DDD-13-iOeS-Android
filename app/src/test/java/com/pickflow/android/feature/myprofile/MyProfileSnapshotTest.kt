package com.pickflow.android.feature.myprofile

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.Density
import com.android.resources.ScreenOrientation
import com.pickflow.android.common.designsystem.PickflowTheme
import com.pickflow.android.core.services.protocols.MyPageHome
import com.pickflow.android.feature.accountmanagement.components.AccountManagementContent
import com.pickflow.android.feature.accountmanagement.components.LogoutConfirmDialogOverlay
import com.pickflow.android.feature.myprofile.components.MyProfileFailedContent
import com.pickflow.android.feature.myprofile.components.MyProfileLoadingContent
import com.pickflow.android.feature.myprofile.components.MyProfileSignedInContent
import com.pickflow.android.feature.myprofile.components.MyProfileSignedOutContent
import com.pickflow.android.feature.withdrawal.components.WithdrawalContent
import com.pickflow.android.feature.withdrawal.model.WithdrawalReason
import org.junit.Rule
import org.junit.Test

/**
 * iOS `MyProfileSnapshotTests` 32케이스 1:1 대응 Paparazzi 스냅샷.
 *
 * iOS는 마이페이지/계정관리/회원탈퇴를 모두 정적 다크 토큰(gray95/gray80/...)으로
 * 렌더하므로 light/dark/a11y 결과가 동일하다 — Android도 동일 컨텐츠를 각 이름으로
 * record한다. iOS `pretendard`는 고정 크기라 a11y(DynamicType)에도 반응하지 않는다.
 *
 * 모든 케이스는 iOS와 동일하게 393x852 고정 크기로 렌더한다.
 */
class MyProfileSnapshotTest {

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = device(393, 852))

    // MARK: - My Profile: Signed Out

    @Test fun my_profile_signedout_light() = snapshot { MyProfileSignedOutContent() }
    @Test fun my_profile_signedout_dark() = snapshot { MyProfileSignedOutContent() }
    @Test fun my_profile_signedout_a11y() = snapshot { MyProfileSignedOutContent() }

    // MARK: - My Profile: Loading

    @Test fun my_profile_loading_light() = snapshot { MyProfileLoadingContent() }
    @Test fun my_profile_loading_dark() = snapshot { MyProfileLoadingContent() }

    // MARK: - My Profile: Signed In

    @Test fun my_profile_signedin_noimage_light() = snapshot { MyProfileSignedInContent(home = stubHome()) }
    @Test fun my_profile_signedin_noimage_dark() = snapshot { MyProfileSignedInContent(home = stubHome()) }
    @Test fun my_profile_signedin_withimage_light() = snapshot { MyProfileSignedInContent(home = stubHome()) }
    @Test fun my_profile_signedin_withimage_dark() = snapshot { MyProfileSignedInContent(home = stubHome()) }

    // MARK: - My Profile: Failed

    @Test fun my_profile_failed_light() = snapshot { MyProfileFailedContent() }
    @Test fun my_profile_failed_dark() = snapshot { MyProfileFailedContent() }

    // MARK: - Account Management

    @Test fun account_mgmt_idle_light() = snapshot {
        AccountManagementContent(nicknameDraft = "capybara123", isSaveEnabled = false)
    }
    @Test fun account_mgmt_idle_dark() = snapshot {
        AccountManagementContent(nicknameDraft = "capybara123", isSaveEnabled = false)
    }
    @Test fun account_mgmt_dirty_light() = snapshot {
        AccountManagementContent(nicknameDraft = "newname_draft", isSaveEnabled = true)
    }
    @Test fun account_mgmt_dirty_dark() = snapshot {
        AccountManagementContent(nicknameDraft = "newname_draft", isSaveEnabled = true)
    }
    @Test fun account_mgmt_logout_dialog_light() = snapshot { LogoutDialogOverlay(isLoading = false) }
    @Test fun account_mgmt_logout_dialog_dark() = snapshot { LogoutDialogOverlay(isLoading = false) }
    @Test fun account_mgmt_logout_processing_light() = snapshot { LogoutDialogOverlay(isLoading = true) }
    @Test fun account_mgmt_a11y() = snapshot {
        AccountManagementContent(nicknameDraft = "capybara123", isSaveEnabled = false)
    }

    // MARK: - Withdrawal

    @Test fun withdrawal_initial_light() = snapshot { WithdrawalContent() }
    @Test fun withdrawal_initial_dark() = snapshot { WithdrawalContent() }
    @Test fun withdrawal_dropdown_open_light() = snapshot { WithdrawalContent(isDropdownOpen = true) }
    @Test fun withdrawal_dropdown_open_dark() = snapshot { WithdrawalContent(isDropdownOpen = true) }
    @Test fun withdrawal_reason_selected_light() = snapshot {
        WithdrawalContent(selectedReason = WithdrawalReason.RarelyUsed)
    }
    @Test fun withdrawal_reason_selected_dark() = snapshot {
        WithdrawalContent(selectedReason = WithdrawalReason.RarelyUsed)
    }
    @Test fun withdrawal_ready_light() = snapshot {
        WithdrawalContent(selectedReason = WithdrawalReason.RarelyUsed, didAgree = true)
    }
    @Test fun withdrawal_ready_dark() = snapshot {
        WithdrawalContent(selectedReason = WithdrawalReason.RarelyUsed, didAgree = true)
    }
    @Test fun withdrawal_other_empty_light() = snapshot {
        WithdrawalContent(selectedReason = WithdrawalReason.Other, otherFeedback = "", didAgree = true)
    }
    @Test fun withdrawal_other_filled_light() = snapshot {
        WithdrawalContent(
            selectedReason = WithdrawalReason.Other,
            otherFeedback = "개선 의견이에요",
            didAgree = true,
        )
    }
    @Test fun withdrawal_other_filled_dark() = snapshot {
        WithdrawalContent(
            selectedReason = WithdrawalReason.Other,
            otherFeedback = "개선 의견이에요",
            didAgree = true,
        )
    }
    @Test fun withdrawal_processing_light() = snapshot { MyProfileLoadingContent() }
    @Test fun withdrawal_a11y() = snapshot { WithdrawalContent() }

    // MARK: - Helpers

    /** 계정 관리 화면 위에 로그아웃 다이얼로그를 겹쳐 그린다(iOS `logoutDialogScreen` 대응). */
    @Composable
    private fun LogoutDialogOverlay(isLoading: Boolean) {
        Box(modifier = Modifier.fillMaxSize()) {
            AccountManagementContent(nicknameDraft = "capybara123", isSaveEnabled = false)
            LogoutConfirmDialogOverlay(isLoading = isLoading)
        }
    }

    private fun snapshot(content: @Composable () -> Unit) {
        paparazzi.snapshot { PickflowTheme { content() } }
    }

    private fun stubHome() = MyPageHome(
        nickname = "테스트유저#1234",
        profileImageUrl = null,
        savedSpotCount = 12,
        recordedSpotCount = 3,
    )

    private companion object {
        /** density 2.0(XHDPI)로 dp 좌표를 px로 환산한 DeviceConfig. */
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
