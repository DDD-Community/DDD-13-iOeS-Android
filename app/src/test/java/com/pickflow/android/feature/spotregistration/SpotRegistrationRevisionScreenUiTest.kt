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
        source = SpotSource.User,
        updatedAt = "2026-08-06T10:00:00Z",
    )

    private fun setScreen(viewModel: SpotRegistrationViewModel) {
        composeRule.setContent {
            PickflowTheme {
                SpotRegistrationScreen(
                    onBack = {},
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
        composeRule.onNodeWithText("스팟 수정").assertIsDisplayed()
        composeRule.onNodeWithTag("registration-submit").assertTextContains("다시 신청")
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
                updatedAt = "2026-08-06T10:01:00Z",
            )
        val viewModel = viewModel(service)
        viewModel.loadRevision(SPOT_ID)
        setScreen(viewModel)
        composeRule.onNodeWithTag("registration-submit").assertIsEnabled().performClick()

        composeRule.onNodeWithTag("registration-resubmit-sheet").assertIsDisplayed()
        coVerify(exactly = 0) { service.update(any(), any(), any()) }
        coVerify(exactly = 0) { service.requestOpen(any()) }
        composeRule.onNodeWithTag("registration-resubmit-confirm").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("registration-resubmit-sheet").assertDoesNotExist()
        composeRule.onNodeWithTag("registration-name").assertTextContains("기존 노을 스팟")
        coVerify(exactly = 1) { service.update(SPOT_ID, any(), null) }
        coVerify(exactly = 1) { service.requestOpen(SPOT_ID) }
    }

    @Test
    fun resubmit_cancel_preserves_form() {
        val service = mockk<MySpotService>()
        coEvery { service.detail(SPOT_ID) } returns rejectedDetail()
        val replacement = ImagePayload(
            bytes = byteArrayOf(1, 2, 3),
            mimeType = "image/jpeg",
            filename = "replacement.jpg",
        )
        val viewModel = viewModel(service)
        viewModel.loadRevision(SPOT_ID)
        viewModel.setSpotName("수정 중인 노을 스팟")
        viewModel.setImagePayload(replacement, previewUri = "content://replacement")
        setScreen(viewModel)
        composeRule.onNodeWithTag("registration-submit").assertIsEnabled().performClick()

        composeRule.onNodeWithTag("registration-resubmit-sheet").assertIsDisplayed()
        composeRule.onNodeWithTag("registration-resubmit-cancel").performClick()

        composeRule.onNodeWithTag("registration-resubmit-sheet").assertDoesNotExist()
        composeRule.onNodeWithTag("registration-name").assertTextContains("수정 중인 노을 스팟")
        composeRule.onNodeWithContentDescription("선택한 스팟 사진").assertIsDisplayed()
        composeRule.runOnIdle {
            assertEquals("수정 중인 노을 스팟", viewModel.spotName.value)
            assertEquals("content://replacement", viewModel.selectedImageUri.value)
            assertSame(replacement, viewModel.imagePayload.value)
        }
        coVerify(exactly = 0) { service.update(any(), any(), any()) }
        coVerify(exactly = 0) { service.requestOpen(any()) }
    }

    private companion object {
        const val SPOT_ID = 41L
    }
}
