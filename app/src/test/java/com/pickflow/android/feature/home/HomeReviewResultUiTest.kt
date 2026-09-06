package com.pickflow.android.feature.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.pickflow.android.app.navigation.HomeTab
import com.pickflow.android.common.designsystem.PickflowTheme
import com.pickflow.android.core.services.protocols.ReviewDecision
import com.pickflow.android.core.services.protocols.ReviewResult
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Phase B RED contract:
 *
 * - `HomeBottomNavigation(HomeTab, Boolean, (HomeTab) -> Unit)` owns the saved-tab indicator.
 * - `ReviewResultSnackbar(ReviewResult, (Long) -> Unit, () -> Unit)` owns decision-specific copy.
 * - Closing a snackbar is local dismissal only; acknowledgement has no callback on this component.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h950dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HomeReviewResultUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun pending_request_shows_saved_indicator() {
        composeRule.setContent {
            PickflowTheme {
                HomeBottomNavigation(
                    selectedTab = HomeTab.EXPLORE,
                    hasSavedIndicator = true,
                    onTabSelected = { _: HomeTab -> },
                )
            }
        }

        composeRule.onNodeWithTag("home-saved-indicator").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("확인하지 않은 검수 결과 있음")
            .assertIsDisplayed()
    }

    @Test
    fun unseen_approval_keeps_indicator() {
        composeRule.setContent {
            PickflowTheme {
                HomeBottomNavigation(
                    selectedTab = HomeTab.EXPLORE,
                    hasSavedIndicator = true,
                    onTabSelected = { _: HomeTab -> },
                )
            }
        }

        composeRule.onNodeWithTag("home-saved-indicator").assertIsDisplayed()
    }

    @Test
    fun approved_snackbar_opens_published_detail() {
        var openedSpotId: Long? = null
        val result = result(
            resultId = 1L,
            spotId = 41L,
            decision = ReviewDecision.APPROVED,
        )

        composeRule.setContent {
            PickflowTheme {
                ReviewResultSnackbar(
                    result = result,
                    onOpenResult = { spotId: Long -> openedSpotId = spotId },
                    onClose = {},
                )
            }
        }

        composeRule.onNodeWithTag("review-snackbar-approved").assertIsDisplayed()
        composeRule.onNodeWithText("바로 가기").assertIsDisplayed()
        composeRule.onNodeWithTag("review-snackbar-action").performClick()

        assertEquals(41L, openedSpotId)
    }

    @Test
    fun rejected_snackbar_opens_rejected_detail() {
        var openedSpotId: Long? = null
        val result = result(
            resultId = 2L,
            spotId = 42L,
            decision = ReviewDecision.REJECTED,
        )

        composeRule.setContent {
            PickflowTheme {
                ReviewResultSnackbar(
                    result = result,
                    onOpenResult = { spotId: Long -> openedSpotId = spotId },
                    onClose = {},
                )
            }
        }

        composeRule.onNodeWithTag("review-snackbar-rejected").assertIsDisplayed()
        composeRule.onNodeWithText("확인하기").assertIsDisplayed()
        composeRule.onNodeWithTag("review-snackbar-action").performClick()

        assertEquals(42L, openedSpotId)
    }

    @Test
    fun closing_snackbar_does_not_acknowledge() {
        var isVisible by mutableStateOf(true)
        var openedSpotId: Long? = null
        var closeCount = 0
        val result = result(
            resultId = 3L,
            spotId = 43L,
            decision = ReviewDecision.APPROVED,
        )

        composeRule.setContent {
            PickflowTheme {
                HomeBottomNavigation(
                    selectedTab = HomeTab.EXPLORE,
                    hasSavedIndicator = true,
                    onTabSelected = { _: HomeTab -> },
                )
                if (isVisible) {
                    ReviewResultSnackbar(
                        result = result,
                        onOpenResult = { spotId: Long -> openedSpotId = spotId },
                        onClose = {
                            closeCount += 1
                            isVisible = false
                        },
                    )
                }
            }
        }

        composeRule.onNodeWithTag("review-snackbar-close").performClick()

        composeRule.onNodeWithTag("review-snackbar-approved").assertDoesNotExist()
        composeRule.onNodeWithTag("home-saved-indicator").assertIsDisplayed()
        assertEquals(1, closeCount)
        assertEquals(null, openedSpotId)
    }

    @Test
    fun acknowledged_result_hides_indicator() {
        composeRule.setContent {
            PickflowTheme {
                HomeBottomNavigation(
                    selectedTab = HomeTab.EXPLORE,
                    hasSavedIndicator = false,
                    onTabSelected = { _: HomeTab -> },
                )
            }
        }

        composeRule.onNodeWithTag("home-saved-indicator").assertDoesNotExist()
    }

    private fun result(
        resultId: Long,
        spotId: Long,
        decision: ReviewDecision,
    ) = ReviewResult(
        resultId = resultId,
        spotId = spotId,
        decision = decision,
        occurredAt = "2026-08-06T12:00:00Z",
    )
}
