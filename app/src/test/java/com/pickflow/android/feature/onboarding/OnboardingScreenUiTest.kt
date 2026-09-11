package com.pickflow.android.feature.onboarding

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.unit.Density
import com.pickflow.android.common.designsystem.PickflowTheme
import com.pickflow.android.core.services.protocols.OnboardingCompletionStore
import com.pickflow.android.feature.onboarding.components.ONBOARDING_CTA_HEIGHT
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
                OnboardingScreen(viewModel = viewModel(), onFinished = {})
            }
        }
        composeRule.onNodeWithTag("onboarding-screen").assertIsDisplayed()
        // 하단 패널은 고정 1개뿐이므로 CTA 노드도 정확히 1개다.
        composeRule.onNodeWithText("다음으로").assertIsDisplayed()
    }

    @Test
    fun swipe_advances_page() {
        val vm = viewModel()
        composeRule.setContent {
            PickflowTheme {
                OnboardingScreen(viewModel = vm, onFinished = {})
            }
        }
        composeRule.onNodeWithTag("onboarding-screen").performTouchInput { swipeLeft() }
        composeRule.waitForIdle()
        // 좌측 스와이프 → 드래그 스냅 → ViewModel.setPage(1) 동기화.
        assertEquals(1, vm.pageIndex.value)
    }

    @Test
    fun cta_advances_to_next_page() {
        val vm = viewModel()
        composeRule.setContent {
            PickflowTheme {
                OnboardingScreen(viewModel = vm, onFinished = {})
            }
        }
        composeRule.onNodeWithText("다음으로").performClick()
        composeRule.waitForIdle()
        assertEquals(1, vm.pageIndex.value)
    }

    // MARK: - PV-137 CTA 높이 회귀
    //
    // 하단 패널이 화면의 40% weight 로 고정돼 있어, 콘텐츠가 그 안에 안 들어가면
    // 마지막 자식인 CTA 가 남은 높이만 받아 납작하게 눌렸다. 눌리기 전에는 아무
    // 예외도 나지 않고 테스트도 통과하므로 **실측 높이로만** 잡힌다.

    /** 제보 기기(Galaxy S9+ 411x846dp)에서 내비바 inset 이 빠진 유효 높이. 수정 전 46dp. */
    @Test
    @Config(sdk = [34], qualifiers = "w411dp-h725dp-xhdpi")
    fun cta_keeps_full_height_on_short_screen() = assertCtaHeight()

    /** 더 짧은 화면. 수정 전 12dp 까지 눌렸다. */
    @Test
    @Config(sdk = [34], qualifiers = "w360dp-h640dp-xhdpi")
    fun cta_keeps_full_height_on_very_short_screen() = assertCtaHeight()

    /** 큰 글자 크기. 수정 전 8dp 까지 눌렸다(a11y 스냅샷에도 박혀 있었다). */
    @Test
    @Config(sdk = [34], qualifiers = "w390dp-h844dp-xhdpi")
    fun cta_keeps_full_height_at_large_font_scale() = assertCtaHeight(fontScale = 1.8f)

    private fun assertCtaHeight(fontScale: Float = 1f) {
        composeRule.setContent {
            CompositionLocalProvider(
                LocalDensity provides Density(
                    density = LocalDensity.current.density,
                    fontScale = fontScale,
                ),
            ) {
                PickflowTheme {
                    OnboardingScreen(viewModel = viewModel(), onFinished = {})
                }
            }
        }
        val bounds = composeRule.onNodeWithText("다음으로").fetchSemanticsNode().boundsInRoot
        val height = with(composeRule.density) { (bounds.bottom - bounds.top).toDp() }
        assertEquals(ONBOARDING_CTA_HEIGHT, height)
    }

    @Test
    fun cta_on_last_page_finishes_onboarding() {
        var finished = false
        val vm = viewModel().apply { setPage(3) }
        composeRule.setContent {
            PickflowTheme {
                OnboardingScreen(viewModel = vm, onFinished = { finished = true })
            }
        }
        composeRule.onNodeWithText("시작하기").performClick()
        composeRule.waitForIdle()
        assertTrue(finished)
    }
}
