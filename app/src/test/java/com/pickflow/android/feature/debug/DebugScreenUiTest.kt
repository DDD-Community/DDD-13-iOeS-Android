package com.pickflow.android.feature.debug

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.pickflow.android.common.designsystem.PickflowTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DebugScreenUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun renders_debug_screen() {
        composeRule.setContent {
            PickflowTheme { DebugScreen(onBack = {}) }
        }
        composeRule.onNodeWithTag("debug-screen").assertIsDisplayed()
        composeRule.onNodeWithText("Debug").assertIsDisplayed()
    }
}
