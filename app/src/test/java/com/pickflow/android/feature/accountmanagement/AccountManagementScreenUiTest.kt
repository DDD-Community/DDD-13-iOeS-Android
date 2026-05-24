package com.pickflow.android.feature.accountmanagement

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.pickflow.android.common.designsystem.PickflowTheme
import com.pickflow.android.core.services.protocols.AuthService
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

    private fun viewModel() = AccountManagementViewModel(mockk<AuthService>(relaxed = true))

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
