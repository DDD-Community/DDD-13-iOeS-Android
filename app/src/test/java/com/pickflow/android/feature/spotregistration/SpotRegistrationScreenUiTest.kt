package com.pickflow.android.feature.spotregistration

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.pickflow.android.common.designsystem.PickflowTheme
import com.pickflow.android.core.services.protocols.LocationService
import com.pickflow.android.core.services.protocols.MySpotService
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
class SpotRegistrationScreenUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun renders_registration_form() {
        val vm = SpotRegistrationViewModel(
            mockk<MySpotService>(relaxed = true),
            mockk<LocationService>(relaxed = true),
        )
        composeRule.setContent {
            PickflowTheme {
                SpotRegistrationScreen(
                    onBack = {},
                    onOpenSearch = {},
                    onRegistered = {},
                    viewModel = vm,
                )
            }
        }
        composeRule.onNodeWithTag("spotregistration-screen").assertIsDisplayed()
        composeRule.onNodeWithTag("registration-submit").assertExists()
        composeRule.onNodeWithText("스팟 등록").assertExists()
    }
}
