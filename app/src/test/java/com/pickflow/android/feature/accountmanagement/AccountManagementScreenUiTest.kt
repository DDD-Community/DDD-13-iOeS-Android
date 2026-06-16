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
@Config(sdk = [34])
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
        return AccountManagementViewModel(authService, userService)
    }

    @Test
    fun renders_account_rows() {
        composeRule.setContent {
            PickflowTheme {
                AccountManagementScreen(onBack = {}, onSignedOut = {}, viewModel = viewModel())
            }
        }
        composeRule.onNodeWithTag("accountmanagement-screen").assertIsDisplayed()
        composeRule.onNodeWithTag("account-logout").assertIsDisplayed()
        composeRule.onNodeWithTag("account-withdraw").assertIsDisplayed()
    }

    @Test
    fun withdraw_tap_shows_confirm_dialog() {
        composeRule.setContent {
            PickflowTheme {
                AccountManagementScreen(onBack = {}, onSignedOut = {}, viewModel = viewModel())
            }
        }
        composeRule.onNodeWithTag("account-withdraw").performClick()
        composeRule.onNodeWithTag("account-withdraw-confirm").assertIsDisplayed()
    }
}
