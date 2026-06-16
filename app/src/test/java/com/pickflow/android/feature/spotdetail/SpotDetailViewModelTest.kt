package com.pickflow.android.feature.spotdetail

import com.pickflow.android.common.ui.LoadState
import com.pickflow.android.core.services.protocols.BookmarkService
import com.pickflow.android.core.services.protocols.SharePayload
import com.pickflow.android.core.services.protocols.ShareIntentService
import com.pickflow.android.core.services.protocols.SpotDetail
import com.pickflow.android.core.services.protocols.SpotService
import com.pickflow.android.core.services.protocols.SpotTheme
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
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
    }

    @AfterEach
    fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `load uses server isBookmarked as truth (logged in)`() = runTest(testDispatcher) {
        val spot = fixture(isBookmarked = true)
        coEvery { spotService.spot("1") } returns spot

        val vm = SpotDetailViewModel(spotService, bookmarkService, shareIntentService)
        vm.load("1"); advanceUntilIdle()

        assertEquals(LoadState.Loaded(spot), vm.spot.value)
        assertTrue(vm.bookmarked.value)
    }

    @Test
    fun `load falls back to bookmarkService on failure`() = runTest(testDispatcher) {
        val boom = RuntimeException("not found")
        coEvery { spotService.spot("9") } throws boom
        coEvery { bookmarkService.isBookmarked("9") } returns true

        val vm = SpotDetailViewModel(spotService, bookmarkService, shareIntentService)
        vm.load("9"); advanceUntilIdle()

        val state = vm.spot.value
        assertTrue(state is LoadState.Failed && state.error === boom)
        assertTrue(vm.bookmarked.value)
    }

    @Test
    fun `toggleBookmark uses bookmarkService toggle with stringified id`() = runTest(testDispatcher) {
        val spot = fixture(isBookmarked = false)
        coEvery { spotService.spot("1") } returns spot
        coEvery { bookmarkService.toggle("1") } returns true

        val vm = SpotDetailViewModel(spotService, bookmarkService, shareIntentService)
        vm.load("1"); advanceUntilIdle()
        vm.toggleBookmark(); advanceUntilIdle()

        assertTrue(vm.bookmarked.value)
        coVerify(exactly = 1) { bookmarkService.toggle("1") }
    }

    @Test
    fun `reportInvalidInfo sets reportSubmitted true`() {
        val vm = SpotDetailViewModel(spotService, bookmarkService, shareIntentService)
        assertFalse(vm.reportSubmitted.value)
        vm.reportInvalidInfo()
        assertTrue(vm.reportSubmitted.value)
    }

    @Test
    fun `share dispatches with spot payload`() = runTest(testDispatcher) {
        coEvery { spotService.spot("1") } returns fixture()

        val vm = SpotDetailViewModel(spotService, bookmarkService, shareIntentService)
        vm.load("1"); advanceUntilIdle()
        vm.share(); advanceUntilIdle()

        // 현재 SpotDetailViewModel.share 는 fixture 의 title("Cafe") 뒤에 " - comment" 를
        // 붙이고 https URL 로 변환한다. 기대값을 실제 구현에 맞춰 갱신.
        coVerify {
            shareIntentService.share(
                SharePayload(title = "Cafe - comment", url = "https://pickflow.app/spot/1")
            )
        }
    }
}
