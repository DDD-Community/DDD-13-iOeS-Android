package com.pickflow.android.feature.spotdetail

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
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
        coEvery { bookmarkService.isBookmarked("s1") } returns false
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
}
