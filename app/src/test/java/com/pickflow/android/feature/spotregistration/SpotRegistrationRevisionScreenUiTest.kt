package com.pickflow.android.feature.spotregistration

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.pickflow.android.common.designsystem.PickflowTheme
import com.pickflow.android.core.services.protocols.ImagePayload
import com.pickflow.android.core.services.protocols.LocationService
import com.pickflow.android.core.services.protocols.MySpotDetail
import com.pickflow.android.core.services.protocols.MySpotService
import com.pickflow.android.core.services.protocols.MySpotStatus
import com.pickflow.android.core.services.protocols.RejectionReason
import com.pickflow.android.core.services.protocols.SpotRejection
import com.pickflow.android.core.services.protocols.MySpotTransitionResult
import com.pickflow.android.core.services.protocols.MySpotUpdateResult
import com.pickflow.android.core.services.protocols.SpotSource
import com.pickflow.android.core.services.protocols.SpotTheme
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h950dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SpotRegistrationRevisionScreenUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun viewModel(mySpotService: MySpotService) = SpotRegistrationViewModel(
        mySpotService = mySpotService,
        locationService = mockk<LocationService>(relaxed = true),
    )

    private fun rejectedDetail() = MySpotDetail(
        id = SPOT_ID,
        name = "기존 노을 스팟",
        theme = SpotTheme.YUNSEUL,
        imageUrl = "https://cdn.example.com/$SPOT_ID.jpg",
        latitude = 37.55,
        longitude = 127.01,
        address = "서울특별시 용산구 노을길 41",
        capturedDate = "2026-05-20",
        capturedTime = "19:40",
        comment = "기존 코멘트",
        status = MySpotStatus.REJECTED,
        rejection = SpotRejection(
            reason = RejectionReason.LOW_QUALITY,
            reasonLabel = "사진 상태 불량",
            guideMessage = "사진이 흐려요",
            detail = null,
            rejectedAt = "2026-08-06T10:00:00Z",
        ),
        recommendationCount = 3L,
        isRecommended = false,
        isMySpot = true,
        source = SpotSource.User,
    )

    private fun setScreen(
        viewModel: SpotRegistrationViewModel,
        onBack: () -> Unit = {},
    ) {
        composeRule.setContent {
            PickflowTheme {
                SpotRegistrationScreen(
                    onBack = onBack,
                    onOpenSearch = {},
                    onRegistered = {},
                    viewModel = viewModel,
                )
            }
        }
    }

    @Test
    fun create_initial_renders_empty_form() {
        val viewModel = viewModel(mockk(relaxed = true))

        setScreen(viewModel)

        composeRule.onNodeWithTag("spotregistration-screen").assertIsDisplayed()
        composeRule.onNodeWithText("스팟 등록").assertIsDisplayed()
        composeRule.onNodeWithTag("registration-name").assertTextEquals("")
        composeRule.onNodeWithTag("registration-submit").assertIsNotEnabled()
    }

    @Test
    fun revision_loading_renders_progress() {
        val detail = CompletableDeferred<MySpotDetail>()
        val service = mockk<MySpotService>()
        coEvery { service.detail(SPOT_ID) } coAnswers { detail.await() }
        val viewModel = viewModel(service)
        viewModel.loadRevision(SPOT_ID)

        setScreen(viewModel)

        composeRule.onNodeWithTag("registration-revision-loading").assertIsDisplayed()
        detail.complete(rejectedDetail())
    }

    @Test
    fun revision_failed_renders_error() {
        val service = mockk<MySpotService>()
        coEvery { service.detail(SPOT_ID) } throws IllegalStateException("detail failed")
        val viewModel = viewModel(service)
        viewModel.loadRevision(SPOT_ID)

        setScreen(viewModel)

        composeRule.onNodeWithTag("registration-revision-error").assertIsDisplayed()
        composeRule.onNodeWithText("편집 정보를 불러오지 못했어요.").assertIsDisplayed()
    }

    @Test
    fun revision_loaded_prefills_all_fields() {
        val service = mockk<MySpotService>()
        coEvery { service.detail(SPOT_ID) } returns rejectedDetail()
        val viewModel = viewModel(service)
        viewModel.loadRevision(SPOT_ID)

        setScreen(viewModel)

        composeRule.onNodeWithTag("registration-existing-image").assertIsDisplayed()
        composeRule.onNodeWithTag("registration-name").assertTextContains("기존 노을 스팟")
        composeRule.onNodeWithTag("registration-address").assertTextContains("서울특별시 용산구 노을길 41")
        composeRule.onNodeWithTag("registration-theme-yunseul").assertIsSelected()
        composeRule.onNodeWithTag("registration-date").assertTextContains("5월 20일")
        composeRule.onNodeWithTag("registration-time").assertTextContains("오후 7:40")
        composeRule.onNodeWithTag("registration-comment").assertTextContains("기존 코멘트")
        composeRule.onNodeWithText("스팟 등록").assertIsDisplayed()
        composeRule.onNodeWithTag("registration-submit").assertTextContains("등록")
    }

    @Test
    fun resubmit_keeps_existing_image() {
        val service = mockk<MySpotService>()
        coEvery { service.detail(SPOT_ID) } returns rejectedDetail()
        coEvery { service.update(SPOT_ID, any(), null) } returns
            MySpotUpdateResult(
                spotId = SPOT_ID,
                status = MySpotStatus.REJECTED,
                imageUrl = "https://cdn.example.com/41.jpg",
            )
        coEvery { service.requestOpen(SPOT_ID) } returns
            MySpotTransitionResult(
                spotId = SPOT_ID,
                status = MySpotStatus.RE_REVIEW_PENDING,
            )
        val viewModel = viewModel(service)
        viewModel.loadRevision(SPOT_ID)
        setScreen(viewModel)
        // 등록 버튼 한 번으로 바로 제출한다(확인 시트 없음).
        composeRule.onNodeWithTag("registration-submit").assertIsEnabled().performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("registration-name").assertTextContains("기존 노을 스팟")
        coVerify(exactly = 1) { service.update(SPOT_ID, any(), null) }
        coVerify(exactly = 1) { service.requestOpen(SPOT_ID) }
    }

    @Test
    fun revision_back_asks_before_leaving() {
        var backCount = 0
        val service = mockk<MySpotService>()
        coEvery { service.detail(SPOT_ID) } returns rejectedDetail()
        val viewModel = viewModel(service)
        viewModel.loadRevision(SPOT_ID)
        setScreen(viewModel, onBack = { backCount++ })

        composeRule.onNodeWithTag("registration-back").performClick()

        composeRule.onNodeWithTag("registration-exit-dialog").assertIsDisplayed()
        composeRule.runOnIdle { assertEquals(0, backCount) }

        // 계속하기 = 폼 유지.
        composeRule.onNodeWithTag("registration-exit-continue").performClick()
        composeRule.onNodeWithTag("registration-exit-dialog").assertDoesNotExist()
        composeRule.onNodeWithTag("registration-name").assertTextContains("기존 노을 스팟")
        composeRule.runOnIdle { assertEquals(0, backCount) }

        // 나가기 = 실제 뒤로가기.
        composeRule.onNodeWithTag("registration-back").performClick()
        composeRule.onNodeWithTag("registration-exit-confirm").performClick()
        composeRule.runOnIdle { assertEquals(1, backCount) }
    }

    @Test
    fun create_back_leaves_without_dialog() {
        var backCount = 0
        setScreen(viewModel(mockk(relaxed = true)), onBack = { backCount++ })

        composeRule.onNodeWithTag("registration-back").performClick()

        composeRule.onNodeWithTag("registration-exit-dialog").assertDoesNotExist()
        composeRule.runOnIdle { assertEquals(1, backCount) }
    }

    private companion object {
        const val SPOT_ID = 41L
    }
}
