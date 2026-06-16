package com.pickflow.android.feature.myprofile

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.pickflow.android.common.designsystem.PickflowTheme
import com.pickflow.android.core.services.protocols.AuthService
import com.pickflow.android.core.services.protocols.ExternalAppLauncher
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
class MyProfileScreenUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun viewModel(
        loggedIn: Boolean = true,
        home: MyPageHome = MyPageHome(
            nickname = "테스트유저#1234",
            profileImageUrl = null,
            savedSpotCount = 3,
            recordedSpotCount = 1,
        ),
        throwOnFetch: Boolean = false,
    ): MyProfileViewModel {
        val userService = mockk<UserService>()
        val authService = mockk<AuthService>()
        val launcher = mockk<ExternalAppLauncher>(relaxed = true)
        coEvery { authService.isLoggedIn() } returns loggedIn
        if (throwOnFetch) {
            coEvery { userService.fetchMyPage() } throws RuntimeException("network")
        } else {
            coEvery { userService.fetchMyPage() } returns home
        }
        return MyProfileViewModel(userService, authService, launcher)
    }

    @Test
    fun loaded_state_shows_user_nickname_and_menu_split() {
        composeRule.setContent {
            PickflowTheme { MyProfileScreen(onRequireLogin = {}, viewModel = viewModel()) }
        }
        composeRule.onNodeWithTag("myprofile-screen").assertIsDisplayed()
        composeRule.onNodeWithText("테스트유저#1234").assertIsDisplayed()
        // 메뉴는 verticalScroll 끝 쪽이라 기기에서 안 보일 수 있어 assertExists 로 검증.
        composeRule.onNodeWithTag("myprofile-menu-notice").assertExists()
        composeRule.onNodeWithTag("myprofile-menu-terms").assertExists()
        composeRule.onNodeWithTag("myprofile-menu-privacy").assertExists()
    }

    @Test
    fun failed_state_shows_retry() {
        composeRule.setContent {
            PickflowTheme {
                MyProfileScreen(onRequireLogin = {}, viewModel = viewModel(throwOnFetch = true))
            }
        }
        composeRule.onNodeWithTag("state-failed").assertIsDisplayed()
    }

    @Test
    fun signed_out_shows_login_buttons() {
        composeRule.setContent {
            PickflowTheme {
                MyProfileScreen(onRequireLogin = {}, viewModel = viewModel(loggedIn = false))
            }
        }
        composeRule.onNodeWithTag("myprofile-signedout").assertIsDisplayed()
    }
}
