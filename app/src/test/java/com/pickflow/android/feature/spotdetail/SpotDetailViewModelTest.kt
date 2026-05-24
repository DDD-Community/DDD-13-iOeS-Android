package com.pickflow.android.feature.spotdetail

import com.pickflow.android.common.ui.LoadState
import com.pickflow.android.core.services.protocols.BookmarkService
import com.pickflow.android.core.services.protocols.SharePayload
import com.pickflow.android.core.services.protocols.ShareIntentService
import com.pickflow.android.core.services.protocols.Spot
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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SpotDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var spotService: SpotService
    private lateinit var bookmarkService: BookmarkService
    private lateinit var shareIntentService: ShareIntentService

    private val spot = Spot("s1", "Cafe", SpotTheme.CAFE, 0.0, 0.0)

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
    fun `load fetches spot and bookmark state`() = runTest(testDispatcher) {
        coEvery { spotService.spot("s1") } returns spot
        coEvery { bookmarkService.isBookmarked("s1") } returns true

        val vm = SpotDetailViewModel(spotService, bookmarkService, shareIntentService)
        vm.load("s1"); advanceUntilIdle()

        assertEquals(LoadState.Loaded(spot), vm.spot.value)
        assertTrue(vm.bookmarked.value)
    }

    @Test
    fun `toggleBookmark updates state via service`() = runTest(testDispatcher) {
        coEvery { spotService.spot("s1") } returns spot
        coEvery { bookmarkService.isBookmarked("s1") } returns false
        coEvery { bookmarkService.toggle("s1") } returns true

        val vm = SpotDetailViewModel(spotService, bookmarkService, shareIntentService)
        vm.load("s1"); advanceUntilIdle()
        vm.toggleBookmark(); advanceUntilIdle()

        assertTrue(vm.bookmarked.value)
    }

    @Test
    fun `reportInvalidInfo sets reportSubmitted true`() = runTest(testDispatcher) {
        val vm = SpotDetailViewModel(spotService, bookmarkService, shareIntentService)
        assertTrue(!vm.reportSubmitted.value)
        vm.reportInvalidInfo()
        assertTrue(vm.reportSubmitted.value)
    }

    @Test
    fun `share dispatches with spot payload`() = runTest(testDispatcher) {
        coEvery { spotService.spot("s1") } returns spot
        coEvery { bookmarkService.isBookmarked(any()) } returns false

        val vm = SpotDetailViewModel(spotService, bookmarkService, shareIntentService)
        vm.load("s1"); advanceUntilIdle()
        vm.share(); advanceUntilIdle()

        coVerify {
            shareIntentService.share(
                SharePayload(title = "Cafe", url = "pickflow://spot/s1")
            )
        }
    }
}
