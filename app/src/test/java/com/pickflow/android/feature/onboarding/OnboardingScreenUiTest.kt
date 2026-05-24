package com.pickflow.android.feature.onboarding

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import com.pickflow.android.common.designsystem.PickflowTheme
import com.pickflow.android.core.services.protocols.OnboardingCompletionStore
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h950dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class OnboardingScreenUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun viewModel() = OnboardingViewModel(mockk<OnboardingCompletionStore>(relaxed = true))

    @Test
    fun first_page_renders_with_cta() {
        composeRule.setContent {
            PickflowTheme {
                OnboardingScreen(
                    viewModel = viewModel(),
                    isCarouselAnimating = false,
                    onFinished = {},
                )
            }
        }
        composeRule.onNodeWithTag("onboarding-screen").assertIsDisplayed()
        // 하단 패널은 고정 1개뿐이므로 CTA 노드도 정확히 1개다.
        composeRule.onNodeWithText("시작하기").assertIsDisplayed()
    }

    @Test
    fun swipe_advances_page() {
        val vm = viewModel()
        composeRule.setContent {
            PickflowTheme {
                OnboardingScreen(
                    viewModel = vm,
                    isCarouselAnimating = false,
                    onFinished = {},
                )
            }
        }
        composeRule.onNodeWithTag("onboarding-screen").performTouchInput { swipeLeft() }
        composeRule.waitForIdle()
        // 좌측 스와이프 → 드래그 스냅 → ViewModel.setPage(1) 동기화.
        assertEquals(1, vm.pageIndex.value)
    }

    @Test
    fun cta_finishes_onboarding() {
        var finished = false
        composeRule.setContent {
            PickflowTheme {
                OnboardingScreen(
                    viewModel = viewModel(),
                    isCarouselAnimating = false,
                    onFinished = { finished = true },
                )
            }
        }
        composeRule.onNodeWithText("시작하기").performClick()
        composeRule.waitForIdle()
        assertTrue(finished)
    }
}
