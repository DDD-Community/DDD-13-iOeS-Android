package com.pickflow.android.feature.spotregistration

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.pickflow.android.common.designsystem.PickflowTheme
import com.pickflow.android.core.services.protocols.LocationService
import com.pickflow.android.core.services.protocols.MySpotService
import com.pickflow.android.core.services.protocols.SpotTheme
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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

    private fun registrationViewModel() = SpotRegistrationViewModel(
        mockk<MySpotService>(relaxed = true),
        mockk<LocationService>(relaxed = true),
    )

    private fun setRegistrationContent(vm: SpotRegistrationViewModel) {
        composeRule.setContent {
            PickflowTheme {
                SpotRegistrationScreen(onBack = {}, onOpenSearch = {}, onRegistered = {}, viewModel = vm)
            }
        }
    }

    /** PV59-REG1 — 카테고리 칩 4개가 햇살→윤슬→노을→야경 순으로 렌더된다. */
    @Test
    fun theme_chips_render_four_themes_in_order() {
        setRegistrationContent(registrationViewModel())
        listOf("햇살", "윤슬", "노을", "야경").forEach {
            composeRule.onNodeWithText(it).assertExists()
        }
        assertEquals(
            listOf(SpotTheme.SUNLIGHT, SpotTheme.YUNSEUL, SpotTheme.SUNSET, SpotTheme.NIGHT_VIEW),
            SpotTheme.entries,
        )
    }

    /** PV59-REG2 — 초기값은 미선택. */
    @Test
    fun theme_starts_unselected() {
        val vm = registrationViewModel()
        setRegistrationContent(vm)
        composeRule.waitForIdle()
        assertNull(vm.theme.value)
    }

    /** PV59-REG3 — 등록 폼은 단독 선택. 다른 칩을 고르면 이전 선택이 해제된다. */
    @Test
    fun selecting_another_theme_replaces_the_previous_one() {
        val vm = registrationViewModel()
        setRegistrationContent(vm)
        composeRule.onNodeWithText("햇살").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertEquals(SpotTheme.SUNLIGHT, vm.theme.value)

        composeRule.onNodeWithText("야경").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertEquals(SpotTheme.NIGHT_VIEW, vm.theme.value)
    }

    /** PV59-REG4 — 같은 칩 재탭 시 해제된다. */
    @Test
    fun retapping_the_selected_theme_clears_it() {
        val vm = registrationViewModel()
        setRegistrationContent(vm)
        composeRule.onNodeWithText("윤슬").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertEquals(SpotTheme.YUNSEUL, vm.theme.value)

        composeRule.onNodeWithText("윤슬").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertNull(vm.theme.value)
    }
}
