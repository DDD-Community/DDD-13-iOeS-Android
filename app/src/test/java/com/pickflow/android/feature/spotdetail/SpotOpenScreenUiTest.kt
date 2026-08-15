package com.pickflow.android.feature.spotdetail

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.pickflow.android.common.designsystem.PickflowTheme
import com.pickflow.android.common.ui.LoadState
import com.pickflow.android.core.services.protocols.MySpotDetail
import com.pickflow.android.core.services.protocols.MySpotStatus
import com.pickflow.android.core.services.protocols.RejectionReason
import com.pickflow.android.core.services.protocols.SpotRejection
import com.pickflow.android.core.services.protocols.SpotSource
import com.pickflow.android.core.services.protocols.SpotTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h950dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SpotOpenScreenUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun idle_renders_loading() {
        render(LoadState.Idle)

        composeRule.onNodeWithTag("spot-open-loading").assertIsDisplayed()
    }

    @Test
    fun loading_renders_loading() {
        render(LoadState.Loading)

        composeRule.onNodeWithTag("spot-open-loading").assertIsDisplayed()
    }

    @Test
    fun failed_renders_error() {
        render(LoadState.Failed(RuntimeException("stub failure")))

        composeRule.onNodeWithTag("spot-open-error").assertIsDisplayed()
        composeRule.onNodeWithText("스팟 정보를 불러오지 못했어요.").assertIsDisplayed()
    }

    @Test
    fun draft_opens_request_sheet() {
        var requestOpenCalls = 0
        render(
            state = loaded(MySpotStatus.DRAFT),
            onRequestOpen = { requestOpenCalls += 1 },
        )

        composeRule.onNodeWithTag("spot-status-draft").assertIsDisplayed()
        composeRule.onNodeWithTag("spot-action-request-open").performClick()

        composeRule.onNodeWithTag("spot-open-request-sheet").assertIsDisplayed()
        assertEquals(0, requestOpenCalls)
    }

    @Test
    fun request_sheet_confirms_once() {
        var requestOpenCalls = 0
        render(
            state = loaded(MySpotStatus.DRAFT),
            onRequestOpen = { requestOpenCalls += 1 },
        )

        composeRule.onNodeWithTag("spot-action-request-open").performClick()
        composeRule.onNodeWithTag("spot-open-request-confirm").performClick()
        composeRule.waitForIdle()

        assertEquals(1, requestOpenCalls)
        composeRule.onNodeWithTag("spot-open-request-sheet").assertDoesNotExist()
    }

    @Test
    fun pending_renders_reviewing_actions() {
        render(loaded(MySpotStatus.PENDING))

        composeRule.onNodeWithTag("spot-status-pending").assertIsDisplayed()
        composeRule.onNodeWithText("검수중").assertIsDisplayed()
        composeRule.onNodeWithTag("spot-action-withdraw-request").assertIsDisplayed()
        composeRule.onNodeWithTag("spot-recommendation").assertDoesNotExist()
        composeRule.onNodeWithTag("detail-report").assertDoesNotExist()
    }

    @Test
    fun rereview_pending_matches_reviewing_ui() {
        render(loaded(MySpotStatus.RE_REVIEW_PENDING))

        composeRule.onNodeWithTag("spot-status-re-review-pending").assertIsDisplayed()
        composeRule.onNodeWithText("검수중").assertIsDisplayed()
        composeRule.onNodeWithTag("spot-action-withdraw-request").assertIsDisplayed()
        composeRule.onNodeWithTag("spot-recommendation").assertDoesNotExist()
        composeRule.onNodeWithTag("detail-report").assertDoesNotExist()
    }

    @Test
    fun withdraw_sheet_confirms_once() {
        var withdrawCalls = 0
        render(
            state = loaded(MySpotStatus.PENDING),
            onWithdrawRequest = { withdrawCalls += 1 },
        )

        composeRule.onNodeWithTag("spot-action-withdraw-request").performClick()
        composeRule.onNodeWithTag("spot-withdraw-request-sheet").assertIsDisplayed()
        composeRule.onNodeWithTag("spot-withdraw-request-confirm").performClick()
        composeRule.waitForIdle()

        assertEquals(1, withdrawCalls)
        composeRule.onNodeWithTag("spot-withdraw-request-sheet").assertDoesNotExist()
    }

    @Test
    fun rejected_renders_reason_and_two_actions() {
        render(
            LoadState.Loaded(
                detail(
                    status = MySpotStatus.REJECTED,
                    rejection = SpotRejection(
                        reason = RejectionReason.LOW_QUALITY,
                        reasonLabel = "사진 상태 불량",
                        guideMessage = "장소를 식별할 수 있는 사진이 필요해요.",
                        detail = null,
                        rejectedAt = "2026-08-06T10:00:00Z",
                    ),
                ),
            ),
        )

        composeRule.onNodeWithTag("spot-status-rejected").assertIsDisplayed()
        composeRule.onNodeWithTag("spot-rejection-banner").assertIsDisplayed()
        composeRule.onNodeWithText("장소를 식별할 수 있는 사진이 필요해요.").assertIsDisplayed()
        composeRule.onNodeWithTag("spot-action-withdraw-rejection").assertIsDisplayed()
        composeRule.onNodeWithTag("spot-action-revise").assertIsDisplayed()
    }

    @Test
    fun rejected_withdraw_confirms_once() {
        var withdrawCalls = 0
        render(
            state = loaded(MySpotStatus.REJECTED),
            onWithdrawRejection = { withdrawCalls += 1 },
        )

        composeRule.onNodeWithTag("spot-action-withdraw-rejection").performClick()
        composeRule.onNodeWithTag("spot-withdraw-rejection-sheet").assertIsDisplayed()
        composeRule.onNodeWithTag("spot-withdraw-rejection-confirm").performClick()
        composeRule.waitForIdle()

        assertEquals(1, withdrawCalls)
        composeRule.onNodeWithTag("spot-withdraw-rejection-sheet").assertDoesNotExist()
    }

    @Test
    fun rejected_revise_opens_prefilled_form() {
        var revisedSpotId: Long? = null
        render(
            state = loaded(MySpotStatus.REJECTED),
            onRevise = { revisedSpotId = it },
        )

        composeRule.onNodeWithTag("spot-action-revise").performClick()

        assertEquals(SPOT_ID, revisedSpotId)
    }

    @Test
    fun published_owner_renders_public_actions() {
        render(loaded(MySpotStatus.PUBLISHED))

        composeRule.onNodeWithTag("spot-source-user").assertIsDisplayed()
        composeRule.onNodeWithTag("spot-recommendation").assertIsDisplayed()
        composeRule.onNodeWithTag("spot-action-cancel-open").assertIsDisplayed()
        composeRule.onNodeWithTag("spot-action-delete").assertIsDisplayed()
        composeRule.onNodeWithTag("detail-report").assertIsDisplayed()
    }

    @Test
    fun cancel_open_confirms_once() {
        var cancelCalls = 0
        render(
            state = loaded(MySpotStatus.PUBLISHED),
            onCancelOpen = { cancelCalls += 1 },
        )

        composeRule.onNodeWithTag("spot-action-cancel-open").performClick()
        composeRule.onNodeWithTag("spot-cancel-open-sheet").assertIsDisplayed()
        composeRule.onNodeWithTag("spot-cancel-open-confirm").performClick()
        composeRule.waitForIdle()

        assertEquals(1, cancelCalls)
        composeRule.onNodeWithTag("spot-cancel-open-sheet").assertDoesNotExist()
    }

    @Test
    fun delete_confirms_once() {
        var deleteCalls = 0
        render(
            state = loaded(MySpotStatus.PUBLISHED),
            onDelete = { deleteCalls += 1 },
        )

        composeRule.onNodeWithTag("spot-action-delete").performClick()
        composeRule.onNodeWithTag("spot-delete-sheet").assertIsDisplayed()
        composeRule.onNodeWithTag("spot-delete-confirm").performClick()
        composeRule.waitForIdle()

        assertEquals(1, deleteCalls)
        composeRule.onNodeWithTag("spot-delete-sheet").assertDoesNotExist()
    }

    @Test
    fun curated_renders_source_recommendation_and_report() {
        render(
            LoadState.Loaded(
                detail(
                    status = MySpotStatus.PUBLISHED,
                    source = SpotSource.Curated("한국관광공사"),
                ),
            ),
        )

        composeRule.onNodeWithTag("spot-source-curated").assertIsDisplayed()
        composeRule.onNodeWithText("한국관광공사").assertIsDisplayed()
        composeRule.onNodeWithTag("spot-recommendation").assertIsDisplayed()
        composeRule.onNodeWithTag("detail-report").assertIsDisplayed()
        composeRule.onNodeWithTag("spot-action-request-open").assertDoesNotExist()
        composeRule.onNodeWithTag("spot-action-withdraw-request").assertDoesNotExist()
        composeRule.onNodeWithTag("spot-action-withdraw-rejection").assertDoesNotExist()
        composeRule.onNodeWithTag("spot-action-cancel-open").assertDoesNotExist()
        composeRule.onNodeWithTag("spot-action-delete").assertDoesNotExist()
    }

    @Test
    fun recommendation_toggles_optimistically() {
        var state by mutableStateOf(loaded(MySpotStatus.PUBLISHED))
        var isRecommendationInFlight by mutableStateOf(false)
        var toggleCalls = 0
        composeRule.setContent {
            PickflowTheme {
                SpotOpenDetailContent(
                    state = state,
                    isRecommendationInFlight = isRecommendationInFlight,
                    onToggleRecommendation = {
                        toggleCalls += 1
                        val current = (state as LoadState.Loaded<MySpotDetail>).value
                        state = LoadState.Loaded(
                            current.copy(
                                recommendationCount = current.recommendationCount + 1L,
                                isRecommended = true,
                            ),
                        )
                        isRecommendationInFlight = true
                    },
                )
            }
        }

        composeRule.onNodeWithContentDescription("추천하지 않음").performClick()

        assertEquals(1, toggleCalls)
        composeRule.onNodeWithContentDescription("추천함").assertIsDisplayed()
        composeRule.onNodeWithText("8").assertIsDisplayed()
        composeRule.onNodeWithTag("spot-recommendation").assertIsNotEnabled()
    }

    @Test
    fun recommendation_logged_out_requests_login() {
        var toggleCalls = 0
        render(
            state = loaded(MySpotStatus.PUBLISHED),
            isLoggedIn = false,
            onToggleRecommendation = { toggleCalls += 1 },
        )

        composeRule.onNodeWithTag("spot-recommendation").performClick()

        composeRule.onNodeWithTag("spot-recommendation-login").assertIsDisplayed()
        assertEquals(0, toggleCalls)
    }

    @Test
    fun published_modal_is_shown_once() {
        var showPublishedModal by mutableStateOf(true)
        var acknowledgeCalls = 0
        composeRule.setContent {
            PickflowTheme {
                SpotOpenDetailContent(
                    state = loaded(MySpotStatus.PUBLISHED),
                    showPublishedModal = showPublishedModal,
                    onAcknowledgePublishedModal = {
                        acknowledgeCalls += 1
                        showPublishedModal = false
                    },
                )
            }
        }

        composeRule.onNodeWithTag("spot-published-modal").assertIsDisplayed()
        composeRule.onNodeWithTag("spot-published-modal-confirm").performClick()

        assertEquals(1, acknowledgeCalls)
        composeRule.onNodeWithTag("spot-published-modal").assertDoesNotExist()
        composeRule.onNodeWithTag("spot-source-user").assertIsDisplayed()
    }

    private fun render(
        state: LoadState<MySpotDetail>,
        isTransitionInFlight: Boolean = false,
        isRecommendationInFlight: Boolean = false,
        isLoggedIn: Boolean = true,
        showPublishedModal: Boolean = false,
        onRequestOpen: () -> Unit = {},
        onWithdrawRequest: () -> Unit = {},
        onWithdrawRejection: () -> Unit = {},
        onRevise: (Long) -> Unit = {},
        onCancelOpen: () -> Unit = {},
        onDelete: () -> Unit = {},
        onToggleRecommendation: () -> Unit = {},
        onRequireLogin: () -> Unit = {},
        onAcknowledgePublishedModal: () -> Unit = {},
        onReport: () -> Unit = {},
    ) {
        composeRule.setContent {
            PickflowTheme {
                SpotOpenDetailContent(
                    state = state,
                    isTransitionInFlight = isTransitionInFlight,
                    isRecommendationInFlight = isRecommendationInFlight,
                    isLoggedIn = isLoggedIn,
                    showPublishedModal = showPublishedModal,
                    onRequestOpen = onRequestOpen,
                    onWithdrawRequest = onWithdrawRequest,
                    onWithdrawRejection = onWithdrawRejection,
                    onRevise = onRevise,
                    onCancelOpen = onCancelOpen,
                    onDelete = onDelete,
                    onToggleRecommendation = onToggleRecommendation,
                    onRequireLogin = onRequireLogin,
                    onAcknowledgePublishedModal = onAcknowledgePublishedModal,
                    onReport = onReport,
                )
            }
        }
    }

    private fun loaded(status: MySpotStatus): LoadState<MySpotDetail> =
        LoadState.Loaded(detail(status = status))

    private fun detail(
        status: MySpotStatus,
        rejection: SpotRejection? = null,
        source: SpotSource = SpotSource.User,
    ) = MySpotDetail(
        id = SPOT_ID,
        name = "노을 공원",
        theme = SpotTheme.SUNSET,
        imageUrl = null,
        latitude = 37.0,
        longitude = 127.0,
        address = "서울특별시 마포구 하늘공원로",
        capturedDate = "2026-08-06",
        capturedTime = "19:20",
        comment = "노을이 예뻐요",
        status = status,
        rejection = rejection,
        recommendationCount = 7L,
        isRecommended = false,
        source = source,
        updatedAt = "2026-08-06T10:00:00Z",
    )

    private companion object {
        const val SPOT_ID = 41L
    }
}
