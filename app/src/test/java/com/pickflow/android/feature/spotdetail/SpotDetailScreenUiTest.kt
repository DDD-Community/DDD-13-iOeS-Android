package com.pickflow.android.feature.spotdetail

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.pickflow.android.common.designsystem.PickflowTheme
import com.pickflow.android.feature.home.ReviewResultViewModel
import com.pickflow.android.core.services.protocols.AnalyticsLogger
import com.pickflow.android.core.services.protocols.AuthService
import com.pickflow.android.core.services.protocols.BookmarkService
import com.pickflow.android.core.services.protocols.LikeService
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

    private fun openActionsViewModel() =
        SpotOpenActionsViewModel(mockk(relaxed = true))

    private fun actionsViewModel() =
        SpotDetailActionsViewModel(mockk<ExternalAppLauncher>(relaxed = true))

    private fun fixture(isMySpot: Boolean) = SpotDetail(
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
        isMySpot = isMySpot,
    )

    /** 신고 진입점 검증용 화면 셋업. [loggedIn] 은 `AuthService.isLoggedIn()` 응답. */
    private fun setUpDetail(isMySpot: Boolean, loggedIn: Boolean) {
        val spotService = mockk<SpotService>()
        val authService = mockk<AuthService>()
        coEvery { spotService.spot("s1") } returns fixture(isMySpot)
        coEvery { authService.isLoggedIn() } returns loggedIn
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
                    spotId = "s1",
                    onBack = {},
                    viewModel = vm,
                    actionsViewModel = actionsViewModel(),
                    openActionsViewModel = openActionsViewModel(),
                    reviewResultViewModel = ReviewResultViewModel(mockk(relaxed = true)),
                )
            }
        }
    }

    @Test
    fun my_spot_hides_the_report_entry() {
        setUpDetail(isMySpot = true, loggedIn = true)
        composeRule.onNodeWithTag("detail-report").assertDoesNotExist()
    }

    @Test
    fun other_spot_keeps_the_report_entry() {
        setUpDetail(isMySpot = false, loggedIn = true)
        composeRule.onNodeWithTag("detail-report").assertExists()
    }

    @Test
    fun guest_tapping_report_gets_the_login_prompt_instead_of_the_sheet() {
        setUpDetail(isMySpot = false, loggedIn = false)
        composeRule.onNodeWithTag("detail-report").performScrollTo().performClick()
        composeRule.onNodeWithTag("spotdetail-login-overlay").assertIsDisplayed()
        composeRule.onNodeWithTag("report-text").assertDoesNotExist()
    }

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
            mockk<LikeService>(relaxed = true),
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
                    openActionsViewModel = openActionsViewModel(),
                    reviewResultViewModel = ReviewResultViewModel(mockk(relaxed = true)),
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
            mockk<LikeService>(relaxed = true),
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
                    openActionsViewModel = openActionsViewModel(),
                    reviewResultViewModel = ReviewResultViewModel(mockk(relaxed = true)),
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

    private fun viewModel(
        spotService: SpotService,
        authService: AuthService,
        likeService: LikeService = mockk(relaxed = true),
    ) = SpotDetailViewModel(
        spotService,
        mockk<BookmarkService>(relaxed = true),
        likeService,
        mockk<ShareIntentService>(relaxed = true),
        mockk<SpotReportService>(relaxed = true),
        authService,
        mockk<AnalyticsLogger>(relaxed = true),
    ).apply { likeDebounceMillis = 0L }

    private fun render(vm: SpotDetailViewModel, showRegisteredToast: Boolean = false) {
        composeRule.setContent {
            PickflowTheme {
                SpotDetailScreen(
                    spotId = "1",
                    onBack = {},
                    showRegisteredToast = showRegisteredToast,
                    viewModel = vm,
                    actionsViewModel = actionsViewModel(),
                    openActionsViewModel = openActionsViewModel(),
                    reviewResultViewModel = ReviewResultViewModel(mockk(relaxed = true)),
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
        val likeService = mockk<LikeService>()
        coEvery { likeService.add("1") } returns 8L
        val authService = mockk<AuthService>(relaxed = true)
        coEvery { authService.isLoggedIn() } returns true

        render(viewModel(spotService, authService, likeService))

        composeRule.onNodeWithTag("detail-like").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("spotdetail-toast").fetchSemanticsNodes().isNotEmpty()
        }
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

    // MARK: - PV-143

    /** 추천 토스트는 체크 아이콘 없이 문구만 띄운다. */
    @Test
    fun like_toast_has_no_check_icon() {
        val spotService = mockk<SpotService>()
        coEvery { spotService.spot("1") } returns spot(isLikeable = true)
        val likeService = mockk<LikeService>()
        coEvery { likeService.add("1") } returns 8L
        val authService = mockk<AuthService>(relaxed = true)
        coEvery { authService.isLoggedIn() } returns true

        render(viewModel(spotService, authService, likeService))

        composeRule.onNodeWithTag("detail-like").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("spotdetail-toast").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("이 스팟을 추천했어요.").assertIsDisplayed()
        composeRule.onNodeWithTag("spotdetail-toast-check").assertDoesNotExist()
    }

    /** 반대로 제보 접수 등 기존 토스트는 체크 아이콘을 유지한다. */
    @Test
    fun registered_toast_keeps_the_check_icon() {
        val spotService = mockk<SpotService>()
        coEvery { spotService.spot("1") } returns spot(isLikeable = false)

        render(viewModel(spotService, mockk(relaxed = true)), showRegisteredToast = true)

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("spotdetail-toast").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("나만의 스팟이 등록되었어요!").assertIsDisplayed()
        composeRule.onNodeWithTag("spotdetail-toast-check").assertIsDisplayed()
    }

    /** 추천을 누르면 헤더의 "추천 N" 이 바로 +1 된다(낙관적 반영). */
    @Test
    fun tapping_like_increments_the_header_like_count() {
        val spotService = mockk<SpotService>()
        coEvery { spotService.spot("1") } returns spot(isLikeable = true, likeCount = 7)
        val likeService = mockk<LikeService>()
        coEvery { likeService.add("1") } returns 8L
        val authService = mockk<AuthService>(relaxed = true)
        coEvery { authService.isLoggedIn() } returns true

        render(viewModel(spotService, authService, likeService))

        composeRule.onNodeWithText("노을 · 추천 7").assertIsDisplayed()

        composeRule.onNodeWithTag("detail-like").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("노을 · 추천 8").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("노을 · 추천 8").assertIsDisplayed()
    }

    /** 추천 실패 시 수도 함께 되돌아온다. */
    @Test
    fun failed_like_restores_the_header_like_count() {
        val spotService = mockk<SpotService>()
        coEvery { spotService.spot("1") } returns spot(isLikeable = true, likeCount = 7)
        val likeService = mockk<LikeService>()
        coEvery { likeService.add("1") } throws RuntimeException("net")
        val authService = mockk<AuthService>(relaxed = true)
        coEvery { authService.isLoggedIn() } returns true

        render(viewModel(spotService, authService, likeService))

        composeRule.onNodeWithTag("detail-like").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("잠시 후 다시 시도해주세요.").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("노을 · 추천 7").assertIsDisplayed()
        composeRule.onNodeWithTag("spotdetail-toast-check").assertDoesNotExist()
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
