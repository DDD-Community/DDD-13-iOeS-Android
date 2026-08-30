package com.pickflow.android.feature.accountmanagement

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.pickflow.android.common.designsystem.PickflowTheme
import com.pickflow.android.core.services.protocols.AuthService
import com.pickflow.android.core.services.protocols.MyPageHome
import com.pickflow.android.core.services.protocols.UserService
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h950dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AccountManagementScreenUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun viewModel(): AccountManagementViewModel {
        val authService = mockk<AuthService>(relaxed = true)
        val userService = mockk<UserService>()
        coEvery { userService.fetchMyPage() } returns MyPageHome(
            nickname = "테스트유저#1234",
            profileImageUrl = null,
            savedSpotCount = 0,
            recordedSpotCount = 0,
        )
        return AccountManagementViewModel(authService, userService, mockk(relaxed = true))
    }

    @Test
    fun renders_account_rows() {
        composeRule.setContent {
            PickflowTheme {
                AccountManagementScreen(onBack = {}, onSignedOut = {}, viewModel = viewModel())
            }
        }
        composeRule.onNodeWithTag("accountmanagement-screen").assertIsDisplayed()
        // 로그아웃/회원탈퇴는 연결된 소셜 하단 배치 — 화면 하단이라 존재만 검증.
        composeRule.onNodeWithTag("account-logout").assertExists()
        composeRule.onNodeWithTag("account-withdraw").assertExists()
    }

    @Test
    fun withdraw_tap_invokes_onOpenWithdrawal() {
        // 회원탈퇴는 확인 다이얼로그 대신 탈퇴 안내 화면(WITHDRAWAL 라우트)으로 이동한다.
        var opened = false
        composeRule.setContent {
            PickflowTheme {
                AccountManagementScreen(
                    onBack = {},
                    onSignedOut = {},
                    onOpenWithdrawal = { opened = true },
                    viewModel = viewModel(),
                )
            }
        }
        composeRule.onNodeWithTag("account-withdraw").performClick()
        composeRule.waitForIdle()
        assert(opened) { "onOpenWithdrawal 이 호출되어야 한다" }
    }
}
