package com.pickflow.android.feature.spotdetail

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.onNodeWithText
import com.pickflow.android.common.designsystem.PickflowTheme
import com.pickflow.android.core.services.protocols.AnalyticsLogger
import com.pickflow.android.core.services.protocols.AuthService
import com.pickflow.android.core.services.protocols.BookmarkService
import com.pickflow.android.core.services.protocols.ExternalAppLauncher
import com.pickflow.android.core.services.protocols.ShareIntentService
import com.pickflow.android.core.services.protocols.SpotDetail
import com.pickflow.android.core.services.protocols.SpotReportService
import com.pickflow.android.core.services.protocols.SpotService
import com.pickflow.android.core.services.protocols.SpotTheme
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h950dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SpotDetailScreenUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun actionsViewModel() =
        SpotDetailActionsViewModel(mockk<ExternalAppLauncher>(relaxed = true))

    @Test
    fun loaded_state_shows_spot_name_and_actions() {
        val spotService = mockk<SpotService>()
        val bookmarkService = mockk<BookmarkService>()
        val shareIntentService = mockk<ShareIntentService>(relaxed = true)
        coEvery { spotService.spot("s1") } returns SpotDetail(
            id = 1L,
            name = "상세 스팟",
            comment = "",
            theme = SpotTheme.SUNSET,
            latitude = 37.0,
            longitude = 127.0,
            address = "서울",
            addressRoad = null,
            addressJibun = null,
            imageUrl = null,
            recordedDate = "2026-05-25",
            recordedTime = "18:00",
            weather = null,
            congestion = null,
            sunsetTime = null,
            astronomyDate = null,
            weatherUpdatedAt = null,
            congestionUpdatedAt = null,
            parkingInfo = null,
            bookmarkCount = 0L,
            isBookmarked = false,
            isMySpot = false,
        )
        val vm = SpotDetailViewModel(
            spotService,
            bookmarkService,
            shareIntentService,
            mockk<SpotReportService>(relaxed = true),
            mockk<AuthService>(relaxed = true),
            mockk<AnalyticsLogger>(relaxed = true),
        )

        composeRule.setContent {
            PickflowTheme {
                SpotDetailScreen(
                    spotId = "s1",
                    onBack = {},
                    viewModel = vm,
                    actionsViewModel = actionsViewModel(),
                )
            }
        }
        composeRule.onNodeWithTag("spotdetail-screen").assertIsDisplayed()
        composeRule.onNodeWithText("상세 스팟").assertIsDisplayed()
        composeRule.onNodeWithTag("detail-bookmark").assertIsDisplayed()
    }

    @Test
    fun failed_state_shows_retry() {
        val spotService = mockk<SpotService>()
        val bookmarkService = mockk<BookmarkService>(relaxed = true)
        val shareIntentService = mockk<ShareIntentService>(relaxed = true)
        coEvery { spotService.spot(any()) } throws RuntimeException("not found")
        val vm = SpotDetailViewModel(
            spotService,
            bookmarkService,
            shareIntentService,
            mockk<SpotReportService>(relaxed = true),
            mockk<AuthService>(relaxed = true),
            mockk<AnalyticsLogger>(relaxed = true),
        )

        composeRule.setContent {
            PickflowTheme {
                SpotDetailScreen(
                    spotId = "x",
                    onBack = {},
                    viewModel = vm,
                    actionsViewModel = actionsViewModel(),
                )
            }
        }
        composeRule.onNodeWithTag("spotdetail-error").assertIsDisplayed()
    }

    private fun spot(
        isLikeable: Boolean,
        isLiked: Boolean = false,
        likeCount: Long = 0L,
    ) = SpotDetail(
        id = 1L,
        name = "상세 스팟",
        comment = "",
        theme = SpotTheme.SUNSET,
        latitude = 37.0,
        longitude = 127.0,
        address = "서울",
        addressRoad = null,
        addressJibun = null,
        imageUrl = null,
        recordedDate = "2026-05-25",
        recordedTime = "18:30",
        weather = null,
        congestion = null,
        sunsetTime = null,
        astronomyDate = null,
        weatherUpdatedAt = null,
        congestionUpdatedAt = null,
        parkingInfo = null,
        bookmarkCount = 0L,
        isBookmarked = false,
        isMySpot = false,
        isLikeable = isLikeable,
        isLiked = isLiked,
        likeCount = likeCount,
    )

    private fun viewModel(spotService: SpotService, authService: AuthService) = SpotDetailViewModel(
        spotService,
        mockk<BookmarkService>(relaxed = true),
        mockk<ShareIntentService>(relaxed = true),
        mockk<SpotReportService>(relaxed = true),
        authService,
        mockk<AnalyticsLogger>(relaxed = true),
    )

    private fun render(vm: SpotDetailViewModel) {
        composeRule.setContent {
            PickflowTheme {
                SpotDetailScreen(
                    spotId = "1",
                    onBack = {},
                    viewModel = vm,
                    actionsViewModel = actionsViewModel(),
                )
            }
        }
    }

    @Test
    fun like_button_is_hidden_when_not_likeable() {
        val spotService = mockk<SpotService>()
        coEvery { spotService.spot("1") } returns spot(isLikeable = false)

        render(viewModel(spotService, mockk(relaxed = true)))

        composeRule.onNodeWithTag("detail-bookmark").assertIsDisplayed()
        composeRule.onNodeWithTag("detail-like").assertDoesNotExist()
    }

    @Test
    fun liked_spot_from_response_renders_as_liked() {
        val spotService = mockk<SpotService>()
        coEvery { spotService.spot("1") } returns spot(isLikeable = true, isLiked = true)

        render(viewModel(spotService, mockk(relaxed = true)))

        composeRule.onNodeWithTag("detail-like").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("추천 취소").assertIsDisplayed()
    }

    @Test
    fun tapping_like_shows_toast() {
        val spotService = mockk<SpotService>()
        coEvery { spotService.spot("1") } returns spot(isLikeable = true)
        coEvery { spotService.like("1") } returns Unit
        val authService = mockk<AuthService>(relaxed = true)
        coEvery { authService.isLoggedIn() } returns true

        render(viewModel(spotService, authService))

        composeRule.onNodeWithTag("detail-like").performClick()
        composeRule.onNodeWithTag("spotdetail-toast").assertIsDisplayed()
        composeRule.onNodeWithText("이 스팟을 추천했어요.").assertIsDisplayed()
    }

    @Test
    fun tapping_like_when_logged_out_shows_login_prompt() {
        val spotService = mockk<SpotService>()
        coEvery { spotService.spot("1") } returns spot(isLikeable = true)
        val authService = mockk<AuthService>(relaxed = true)
        coEvery { authService.isLoggedIn() } returns false

        render(viewModel(spotService, authService))

        composeRule.onNodeWithTag("detail-like").performClick()
        composeRule.onNodeWithTag("spotdetail-login-overlay").assertIsDisplayed()
    }

    @Test
    fun header_shows_theme_and_like_count_from_response() {
        val spotService = mockk<SpotService>()
        coEvery { spotService.spot("1") } returns spot(isLikeable = true, likeCount = 7)

        render(viewModel(spotService, mockk(relaxed = true)))

        composeRule.onNodeWithText("노을 · 추천 7").assertIsDisplayed()
        composeRule.onNodeWithText("노을 · 북마크 0").assertDoesNotExist()
    }
}
