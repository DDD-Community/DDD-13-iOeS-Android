package com.pickflow.android.feature.spotdetail

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.pickflow.android.common.designsystem.PickflowTheme
import com.pickflow.android.core.services.protocols.AnalyticsLogger
import com.pickflow.android.core.services.protocols.AuthService
import com.pickflow.android.core.services.protocols.BookmarkService
import com.pickflow.android.core.services.protocols.LikeService
import com.pickflow.android.core.services.protocols.MySpotReleaseStore
import com.pickflow.android.core.services.protocols.MySpotService
import com.pickflow.android.core.services.protocols.MySpotStatus
import com.pickflow.android.core.services.protocols.MySpotTransitionResult
import com.pickflow.android.core.services.protocols.MySpotUnpublishResult
import com.pickflow.android.core.services.protocols.RejectionReason
import com.pickflow.android.core.services.protocols.ShareIntentService
import com.pickflow.android.core.services.protocols.SpotDetail
import com.pickflow.android.core.services.protocols.SpotRejection
import com.pickflow.android.core.services.protocols.SpotReportService
import com.pickflow.android.core.services.protocols.SpotService
import com.pickflow.android.core.services.protocols.SpotSource
import com.pickflow.android.core.services.protocols.SpotTheme
import com.pickflow.android.feature.home.ReviewResultViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 내 스팟 오픈 플로우 — 상세 화면 하나로 합친 뒤의 UI 시나리오.
 * 삭제된 `SpotOpenScreenUiTest` 의 상태별 커버리지를 이 화면 기준으로 옮겼다.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class SpotDetailOpenFlowUiTest {

    /** relaxed mock 은 released()=false 를 돌려줘 토글이 OFF 로 시작한다. 공개 직후 기본값은 ON 이다. */
    private val releaseStore = object : MySpotReleaseStore {
        private val values = mutableMapOf<Long, Boolean>()
        override fun released(spotId: Long): Boolean = values[spotId] ?: true
        override fun setReleased(spotId: Long, released: Boolean) { values[spotId] = released }
    }

    @get:Rule
    val composeRule = createComposeRule()

    private val mySpotService = mockk<MySpotService>(relaxed = true)

    private fun fixture(
        status: MySpotStatus?,
        rejection: SpotRejection? = null,
        isMySpot: Boolean = true,
    ) = SpotDetail(
        id = 41L,
        name = "석촌호수 산책길",
        comment = "노을빛에 반사된 윤슬이 가장 반짝여요.",
        theme = SpotTheme.YUNSEUL,
        latitude = 37.5,
        longitude = 127.1,
        address = "서울특별시 송파구 올림픽로 240",
        addressRoad = null,
        addressJibun = null,
        imageUrl = null,
        recordedDate = "2026-04-11",
        recordedTime = "18:33",
        weather = null,
        congestion = null,
        sunsetTime = null,
        astronomyDate = null,
        weatherUpdatedAt = null,
        congestionUpdatedAt = null,
        parkingInfo = null,
        bookmarkCount = 0L,
        isBookmarked = false,
        isMySpot = isMySpot,
        source = if (isMySpot) SpotSource.User else SpotSource.Curated("한국관광공사"),
        mySpotStatus = status,
        rejection = rejection,
    )

    private var revisedSpotId: Long? = null

    private fun render(spot: SpotDetail) {
        val spotService = mockk<SpotService>()
        coEvery { spotService.spot("41") } returns spot
        val authService = mockk<AuthService>(relaxed = true)
        coEvery { authService.isLoggedIn() } returns true
        val vm = SpotDetailViewModel(
            spotService,
            mockk<BookmarkService>(relaxed = true),
            mockk<LikeService>(relaxed = true),
            mockk<ShareIntentService>(relaxed = true),
            mockk<SpotReportService>(relaxed = true),
            authService,
            mockk<AnalyticsLogger>(relaxed = true),
        )
        composeRule.setContent {
            PickflowTheme {
                SpotDetailScreen(
                    spotId = "41",
                    onBack = {},
                    onReviseMySpot = { revisedSpotId = it },
                    viewModel = vm,
                    actionsViewModel = SpotDetailActionsViewModel(mockk(relaxed = true)),
                    openActionsViewModel = SpotOpenActionsViewModel(mySpotService, releaseStore),
                    reviewResultViewModel = ReviewResultViewModel(mockk(relaxed = true)),
                )
            }
        }
    }

    @Test
    fun draft_shows_my_badge_and_open_button() {
        render(fixture(MySpotStatus.DRAFT))

        composeRule.onNodeWithText("MY 스팟").assertIsDisplayed()
        composeRule.onNodeWithText("내 스팟 오픈하기").assertIsDisplayed()
    }

    @Test
    fun draft_open_button_confirms_and_requests_once() {
        coEvery { mySpotService.requestOpen(41L) } returns
            MySpotTransitionResult(41L, MySpotStatus.PENDING)
        render(fixture(MySpotStatus.DRAFT))

        composeRule.onNodeWithTag("detail-open-spot").performScrollTo().performClick()
        composeRule.onNodeWithTag("spot-open-request-sheet").assertIsDisplayed()
        composeRule.onNodeWithTag("spot-open-request-confirm", useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()

        coVerify(timeout = 3_000, exactly = 1) { mySpotService.requestOpen(41L) }
    }

    @Test
    fun pending_shows_reviewing_badge_and_withdraw_action() {
        coEvery { mySpotService.unpublish(41L) } returns
            MySpotUnpublishResult(41L, MySpotStatus.PENDING, MySpotStatus.DRAFT)
        render(fixture(MySpotStatus.PENDING))

        composeRule.onNodeWithText("검수 중").assertIsDisplayed()
        composeRule.onNodeWithTag("detail-open-spot").performScrollTo().performClick()
        composeRule.onNodeWithTag("spot-withdraw-request-sheet").assertIsDisplayed()
        composeRule.onNodeWithTag("spot-withdraw-request-confirm", useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()

        coVerify(timeout = 3_000, exactly = 1) { mySpotService.unpublish(41L) }
    }

    @Test
    fun rereview_pending_matches_pending_ui() {
        render(fixture(MySpotStatus.RE_REVIEW_PENDING))

        composeRule.onNodeWithText("검수 중").assertIsDisplayed()
        composeRule.onNodeWithText("스팟 오픈 철회").assertIsDisplayed()
    }

    @Test
    fun rejected_shows_banner_and_revise_goes_straight_to_the_form() {
        render(
            fixture(
                MySpotStatus.REJECTED,
                rejection = SpotRejection(
                    reason = RejectionReason.LOW_QUALITY,
                    reasonLabel = "사진 품질이 기준에 못 미쳐요",
                    guideMessage = "더 밝은 시간대에 다시 찍어주세요",
                    detail = null,
                    rejectedAt = "2026-08-06T10:00:00Z",
                ),
            ),
        )

        composeRule.onNodeWithText("오픈 반려").assertIsDisplayed()
        composeRule.onNodeWithTag("detail-rejection-banner").assertIsDisplayed()
        composeRule.onNodeWithText("더 밝은 시간대에 다시 찍어주세요").assertIsDisplayed()

        // 재신청은 배너 안 버튼이다 — 하단 오픈 버튼은 숨는다.
        composeRule.onNodeWithTag("detail-open-spot").assertDoesNotExist()
        composeRule.onNodeWithTag("detail-revise-spot").performScrollTo().performClick()
        assert(revisedSpotId == 41L)
    }

    @Test
    fun rejected_banner_can_be_dismissed_without_a_confirm_sheet() {
        render(fixture(MySpotStatus.REJECTED))

        composeRule.onNodeWithTag("detail-dismiss-rejection").performScrollTo().performClick()

        composeRule.onNodeWithTag("detail-rejection-banner").assertDoesNotExist()
        // 확인 시트를 거치지 않는다 — 서버 상태를 바꾸지 않는 세션 한정 동작이다.
        composeRule.onNodeWithTag("spot-withdraw-request-sheet").assertDoesNotExist()
        composeRule.onNodeWithTag("spot-cancel-open-sheet").assertDoesNotExist()
    }

    @Test
    fun published_owner_has_no_bookmark_button() {
        // 공개 상태는 하단 오픈 버튼을 숨기지만, 그렇다고 남의 스팟이 되는 건 아니다.
        render(fixture(MySpotStatus.PUBLISHED))

        composeRule.onNodeWithTag("detail-bookmark").assertDoesNotExist()
    }

    @Test
    fun rejected_owner_has_no_bookmark_button() {
        render(fixture(MySpotStatus.REJECTED))

        composeRule.onNodeWithTag("detail-bookmark").assertDoesNotExist()
    }

    @Test
    fun published_owner_can_toggle_release_and_delete() {
        // 노출 off/on 왕복 + 삭제까지. 노출 토글은 status 를 바꾸지 않아 확인 시트가 없다.
        coEvery { mySpotService.setReleased(41L, false) } returns false
        coEvery { mySpotService.setReleased(41L, true) } returns true
        render(fixture(MySpotStatus.PUBLISHED))

        // 공개 여부는 하단 오픈 버튼이 아니라 공개 토글이 맡는다.
        composeRule.onNodeWithTag("detail-open-spot").assertDoesNotExist()
        composeRule.onNodeWithTag("detail-publish-switch").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("spot-cancel-open-sheet").assertDoesNotExist()
        coVerify(timeout = 3_000, exactly = 1) { mySpotService.setReleased(41L, false) }

        // 재검수 없이 다시 켤 수 있다.
        composeRule.onNodeWithTag("detail-publish-switch").performScrollTo().performClick()
        composeRule.waitForIdle()
        coVerify(timeout = 3_000, exactly = 1) { mySpotService.setReleased(41L, true) }
        coVerify(exactly = 0) { mySpotService.unpublish(any()) }

        composeRule.onNodeWithTag("detail-delete-spot").performScrollTo().performClick()
        composeRule.onNodeWithTag("spot-delete-sheet").assertIsDisplayed()
        composeRule.onNodeWithTag("spot-delete-confirm", useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()
        coVerify(timeout = 3_000, exactly = 1) { mySpotService.delete(41L) }
    }

    @Test
    fun curated_spot_has_no_open_flow_and_keeps_the_report_entry() {
        render(fixture(status = null, isMySpot = false))

        composeRule.onNodeWithTag("detail-open-spot").assertDoesNotExist()
        composeRule.onNodeWithTag("detail-delete-spot").assertDoesNotExist()
        composeRule.onNodeWithTag("detail-report").assertExists()
    }
}
