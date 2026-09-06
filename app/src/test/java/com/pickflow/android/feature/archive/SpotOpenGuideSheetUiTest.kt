package com.pickflow.android.feature.archive

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.pickflow.android.common.designsystem.PickflowTheme
import com.pickflow.android.feature.archive.components.SpotOpenGuideSheetContent
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Figma `1201-9416` / `1201-9439` — 문구 2줄과 버튼 2개. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h950dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SpotOpenGuideSheetUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun render(onGoToOpen: () -> Unit = {}, onConfirm: () -> Unit = {}) {
        composeRule.setContent {
            PickflowTheme {
                SpotOpenGuideSheetContent(onGoToOpen = onGoToOpen, onConfirm = onConfirm)
            }
        }
    }

    @Test
    fun renders_title_body_and_both_buttons() {
        render()

        composeRule.onNodeWithText("스팟 공개 OPEN!").assertIsDisplayed()
        composeRule.onNodeWithText("내가 기록한 스팟을 다른 유저에게 공개할 수 있어요").assertIsDisplayed()
        composeRule.onNodeWithTag("spot-open-guide-go").assertIsDisplayed()
        composeRule.onNodeWithTag("spot-open-guide-confirm").assertIsDisplayed()
        composeRule.onNodeWithTag("spot-open-guide-illustration").assertIsDisplayed()
    }

    @Test
    fun go_button_reports_only_go() {
        var go = 0
        var confirm = 0
        render(onGoToOpen = { go += 1 }, onConfirm = { confirm += 1 })

        composeRule.onNodeWithTag("spot-open-guide-go").performClick()

        composeRule.runOnIdle {
            assertEquals(1, go)
            assertEquals(0, confirm)
        }
    }

    @Test
    fun confirm_button_reports_only_confirm() {
        var go = 0
        var confirm = 0
        render(onGoToOpen = { go += 1 }, onConfirm = { confirm += 1 })

        composeRule.onNodeWithTag("spot-open-guide-confirm").performClick()

        composeRule.runOnIdle {
            assertEquals(0, go)
            assertEquals(1, confirm)
        }
    }
}
