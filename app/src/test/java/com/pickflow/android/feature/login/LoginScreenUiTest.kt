package com.pickflow.android.feature.login

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.pickflow.android.common.designsystem.PickflowTheme
import com.pickflow.android.core.services.protocols.KakaoAuthProvider
import com.pickflow.android.core.services.protocols.SocialLoginService
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
class LoginScreenUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun viewModel() = LoginViewModel(
        mockk<KakaoAuthProvider>(relaxed = true),
        mockk<SocialLoginService>(relaxed = true),
    )

    @Test
    fun renders_login_entry_points() {
        composeRule.setContent {
            PickflowTheme { LoginScreen(viewModel = viewModel(), onLoggedIn = {}) }
        }
        composeRule.onNodeWithTag("login-screen").assertIsDisplayed()
        composeRule.onNodeWithTag("login-kakao").assertIsDisplayed()
        composeRule.onNodeWithText("카카오로 로그인").assertIsDisplayed()
        composeRule.onNodeWithText("비회원으로 시작하기").assertIsDisplayed()
    }
}
