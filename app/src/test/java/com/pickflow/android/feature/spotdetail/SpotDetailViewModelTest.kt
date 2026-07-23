package com.pickflow.android.feature.spotdetail

import com.pickflow.android.common.ui.LoadState
import com.pickflow.android.common.util.SpotIdCoder
import com.pickflow.android.core.analytics.events.ShareFakedoorAnalyticsEvent
import com.pickflow.android.core.analytics.events.SpotDetailAnalyticsEvent
import com.pickflow.android.core.services.protocols.AnalyticsLogger
import com.pickflow.android.core.services.protocols.AuthService
import com.pickflow.android.core.services.protocols.BookmarkService
import com.pickflow.android.core.services.protocols.SharePayload
import com.pickflow.android.core.services.protocols.ShareIntentService
import com.pickflow.android.core.services.protocols.SpotDetail
import com.pickflow.android.core.services.protocols.SpotReportService
import com.pickflow.android.core.services.protocols.SpotService
import com.pickflow.android.core.services.protocols.SpotTheme
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SpotDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var spotService: SpotService
    private lateinit var bookmarkService: BookmarkService
    private lateinit var shareIntentService: ShareIntentService
    private lateinit var spotReportService: SpotReportService
    private lateinit var authService: AuthService
    private lateinit var analyticsLogger: AnalyticsLogger

    private fun fixture(isBookmarked: Boolean = false, isMySpot: Boolean = false): SpotDetail =
        SpotDetail(
            id = 1L,
            name = "Cafe",
            comment = "comment",
            theme = SpotTheme.SUNSET,
            latitude = 0.0,
            longitude = 0.0,
            address = "addr",
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
            isBookmarked = isBookmarked,
            isMySpot = isMySpot,
        )

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        spotService = mockk()
        bookmarkService = mockk()
        shareIntentService = mockk(relaxed = true)
        spotReportService = mockk(relaxed = true)
        authService = mockk(relaxed = true)
        analyticsLogger = mockk(relaxed = true)
    }

    @AfterEach
    fun tearDown() { Dispatchers.resetMain() }

    private fun vm() = SpotDetailViewModel(
        spotService, bookmarkService, shareIntentService, spotReportService, authService,
        analyticsLogger,
    )

    @Test
    fun `load uses server isBookmarked as truth (logged in)`() = runTest(testDispatcher) {
        val spot = fixture(isBookmarked = true)
        coEvery { spotService.spot("1") } returns spot

        val vm = vm()
        vm.load("1"); advanceUntilIdle()

        assertEquals(LoadState.Loaded(spot), vm.spot.value)
        assertTrue(vm.bookmarked.value)
    }

    @Test
    fun `load falls back to bookmarkService on failure`() = runTest(testDispatcher) {
        val boom = RuntimeException("not found")
        coEvery { spotService.spot("9") } throws boom
        coEvery { bookmarkService.isBookmarked("9") } returns true

        val vm = vm()
        vm.load("9"); advanceUntilIdle()

        val state = vm.spot.value
        assertTrue(state is LoadState.Failed && state.error === boom)
        assertTrue(vm.bookmarked.value)
    }

    @Test
    fun `toggleBookmark adds bookmark with stringified id when logged in`() = runTest(testDispatcher) {
        val spot = fixture(isBookmarked = false)
        coEvery { spotService.spot("1") } returns spot
        coEvery { authService.isLoggedIn() } returns true
        coEvery { bookmarkService.add("1") } returns 1L

        val vm = vm()
        vm.load("1"); advanceUntilIdle()
        vm.toggleBookmark(); advanceUntilIdle()

        assertTrue(vm.bookmarked.value)
        coVerify(exactly = 1) { bookmarkService.add("1") }
    }

    @Test
    fun `toggleBookmark when logged out shows login prompt without server call`() = runTest(testDispatcher) {
        val spot = fixture(isBookmarked = false)
        coEvery { spotService.spot("1") } returns spot
        coEvery { authService.isLoggedIn() } returns false

        val vm = vm()
        vm.load("1"); advanceUntilIdle()
        vm.toggleBookmark(); advanceUntilIdle()

        assertTrue(vm.isLoginRequired.value)
        assertFalse(vm.bookmarked.value)
        coVerify(exactly = 0) { bookmarkService.add(any()) }
    }

    @Test
    fun `reportInvalidInfo sets reportSubmitted true on success`() = runTest(testDispatcher) {
        coEvery { spotService.spot("1") } returns fixture()
        coEvery { spotReportService.report(1L, any()) } returns 10L

        val vm = vm()
        vm.load("1"); advanceUntilIdle()
        assertFalse(vm.reportSubmitted.value)
        vm.reportInvalidInfo("실제 위치가 지도와 달라요"); advanceUntilIdle()

        assertTrue(vm.reportSubmitted.value)
        assertEquals("제보가 접수되었습니다.", vm.toast.value)
    }

    @Test
    fun `reportInvalidInfo emits failure toast on error`() = runTest(testDispatcher) {
        coEvery { spotService.spot("1") } returns fixture()
        coEvery { spotReportService.report(1L, any()) } throws RuntimeException("boom")

        val vm = vm()
        vm.load("1"); advanceUntilIdle()
        vm.reportInvalidInfo("실제 위치가 지도와 달라요"); advanceUntilIdle()

        assertFalse(vm.reportSubmitted.value)
        assertEquals("제보 접수에 실패했어요.", vm.toast.value)
    }

    @Test
    fun `share dispatches with spot payload`() = runTest(testDispatcher) {
        coEvery { spotService.spot("1") } returns fixture()

        val vm = vm()
        vm.load("1"); advanceUntilIdle()
        vm.share(); advanceUntilIdle()

        // share 는 "이름 - 코멘트" 제목 + SpotIdCoder 인코딩 URL 을 전달한다.
        coVerify {
            shareIntentService.share(
                SharePayload(
                    title = "Cafe - comment",
                    url = "https://pickflow-api.us/${SpotIdCoder.encodeSpot(1L)}",
                )
            )
        }
    }

    @Test
    fun `share logs spot_detail_share_btn_tap`() = runTest(testDispatcher) {
        coEvery { spotService.spot("1") } returns fixture()

        val vm = vm()
        vm.load("1"); advanceUntilIdle()
        vm.share(); advanceUntilIdle()

        verify(exactly = 1) { analyticsLogger.log(SpotDetailAnalyticsEvent.SHARE_BUTTON_TAP) }
    }

    @Test
    fun `notifyUpdateRequested logs modal_share_fakedoor_btn_tap and shows toast`() = runTest(testDispatcher) {
        val vm = vm()
        vm.notifyUpdateRequested()

        verify(exactly = 1) { analyticsLogger.log(ShareFakedoorAnalyticsEvent.NOTIFY_BUTTON_TAP) }
        assertEquals("추후 업데이트 시, 가장 먼저 알림 보내드릴게요!", vm.toast.value)
    }
}
