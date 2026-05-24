package com.pickflow.android.feature.myprofile

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.pickflow.android.common.designsystem.PickflowTheme
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

    @Test
    fun loaded_state_shows_user_name() {
        val userService = mockk<UserService>()
        coEvery { userService.fetchUserName() } returns "테스트 유저"
        val vm = MyProfileViewModel(userService)

        composeRule.setContent {
            PickflowTheme { MyProfileScreen(onRequireLogin = {}, viewModel = vm) }
        }
        composeRule.onNodeWithTag("myprofile-screen").assertIsDisplayed()
        composeRule.onNodeWithText("테스트 유저").assertIsDisplayed()
    }

    @Test
    fun failed_state_shows_retry() {
        val userService = mockk<UserService>()
        coEvery { userService.fetchUserName() } throws RuntimeException("network")
        val vm = MyProfileViewModel(userService)

        composeRule.setContent {
            PickflowTheme { MyProfileScreen(onRequireLogin = {}, viewModel = vm) }
        }
        composeRule.onNodeWithTag("state-failed").assertIsDisplayed()
    }
}
