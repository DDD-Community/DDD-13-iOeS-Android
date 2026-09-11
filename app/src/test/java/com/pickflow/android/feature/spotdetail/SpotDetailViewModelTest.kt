package com.pickflow.android.feature.spotdetail

import com.pickflow.android.common.ui.LoadState
import com.pickflow.android.common.util.SpotIdCoder
import com.pickflow.android.core.analytics.events.ShareFakedoorAnalyticsEvent
import com.pickflow.android.core.analytics.events.SpotDetailAnalyticsEvent
import com.pickflow.android.core.services.protocols.AnalyticsLogger
import com.pickflow.android.core.services.protocols.AuthService
import com.pickflow.android.core.services.protocols.BookmarkService
import com.pickflow.android.core.services.protocols.LikeService
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SpotDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var spotService: SpotService
    private lateinit var bookmarkService: BookmarkService
    private lateinit var likeService: LikeService
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
        likeService = mockk()
        shareIntentService = mockk(relaxed = true)
        spotReportService = mockk(relaxed = true)
        authService = mockk(relaxed = true)
        analyticsLogger = mockk(relaxed = true)
    }

    @AfterEach
    fun tearDown() { Dispatchers.resetMain() }

    private fun vm() = SpotDetailViewModel(
        spotService, bookmarkService, likeService, shareIntentService, spotReportService,
        authService, analyticsLogger,
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
    fun `load failure leaves bookmarked false`() = runTest(testDispatcher) {
        val boom = RuntimeException("not found")
        coEvery { spotService.spot("9") } throws boom

        val vm = vm()
        vm.load("9"); advanceUntilIdle()

        val state = vm.spot.value
        assertTrue(state is LoadState.Failed && state.error === boom)
        assertFalse(vm.bookmarked.value)
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
    fun `toggleBookmark ignores the second tap while the first is still in flight`() =
        runTest(testDispatcher) {
            coEvery { spotService.spot("1") } returns fixture(isBookmarked = false)
            coEvery { authService.isLoggedIn() } returns true
            coEvery { bookmarkService.add("1") } coAnswers { delay(100); 1L }

            val vm = vm()
            vm.load("1"); advanceUntilIdle()
            vm.toggleBookmark()
            vm.toggleBookmark() // 연타
            advanceUntilIdle()

            // add/remove 가 교차 실행되지 않아 실패 토스트도 뜨지 않는다.
            coVerify(exactly = 1) { bookmarkService.add("1") }
            coVerify(exactly = 0) { bookmarkService.remove(any()) }
            assertTrue(vm.bookmarked.value)
            assertEquals(null, vm.toast.value)
        }

    @Test
    fun `toggleBookmark accepts a new tap once the previous request finished`() =
        runTest(testDispatcher) {
            coEvery { spotService.spot("1") } returns fixture(isBookmarked = false)
            coEvery { authService.isLoggedIn() } returns true
            coEvery { bookmarkService.add("1") } returns 1L
            coEvery { bookmarkService.remove("1") } returns 0L

            val vm = vm()
            vm.load("1"); advanceUntilIdle()
            vm.toggleBookmark(); advanceUntilIdle()
            vm.toggleBookmark(); advanceUntilIdle()

            coVerify(exactly = 1) { bookmarkService.add("1") }
            coVerify(exactly = 1) { bookmarkService.remove("1") }
            assertFalse(vm.bookmarked.value)
        }

    @Test
    fun `requestReport opens the sheet when logged in`() = runTest(testDispatcher) {
        coEvery { authService.isLoggedIn() } returns true

        var allowed = false
        val vm = vm()
        vm.requestReport { allowed = true }; advanceUntilIdle()

        assertTrue(allowed)
        assertFalse(vm.isLoginRequired.value)
    }

    @Test
    fun `requestReport shows login prompt instead of the sheet when logged out`() =
        runTest(testDispatcher) {
            coEvery { authService.isLoggedIn() } returns false

            var allowed = false
            val vm = vm()
            vm.requestReport { allowed = true }; advanceUntilIdle()

            assertFalse(allowed)
            assertTrue(vm.isLoginRequired.value)
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
        assertEquals("제보가 접수되었습니다.", vm.toast.value?.message)
    }

    @Test
    fun `reportInvalidInfo emits failure toast on error`() = runTest(testDispatcher) {
        coEvery { spotService.spot("1") } returns fixture()
        coEvery { spotReportService.report(1L, any()) } throws RuntimeException("boom")

        val vm = vm()
        vm.load("1"); advanceUntilIdle()
        vm.reportInvalidInfo("실제 위치가 지도와 달라요"); advanceUntilIdle()

        assertFalse(vm.reportSubmitted.value)
        assertEquals("제보 접수에 실패했어요.", vm.toast.value?.message)
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
        assertEquals("추후 업데이트 시, 가장 먼저 알림 보내드릴게요!", vm.toast.value?.message)
    }

    @Test
    fun `load seeds liked from the response isLiked`() = runTest(testDispatcher) {
        coEvery { spotService.spot("1") } returns fixture().copy(isLiked = true, isLikeable = true)

        val vm = vm()
        vm.load("1"); advanceUntilIdle()

        assertTrue(vm.liked.value)
    }

    // MARK: - PV-143 추천 수

    @Test
    fun `likeCount seeds from the detail response`() = runTest(testDispatcher) {
        coEvery { spotService.spot("1") } returns
            fixture().copy(likeCount = 7L, isLiked = false, isLikeable = true)

        val vm = vm()
        vm.load("1"); advanceUntilIdle()

        assertEquals(7, vm.likeCount.value)
    }

    /**
     * 탭하는 순간 디바운스/네트워크를 기다리지 않고 +1 이 보여야 한다.
     *
     * `runCurrent()` 는 가상 시간을 진행시키지 않으므로 디바운스 `delay` 가 아직
     * 걸려 있다 = **서버 요청 전**이다. 파생 StateFlow 가 재계산될 한 턴만 준다.
     */
    @Test
    fun `likeCount increments optimistically before the request goes out`() = runTest(testDispatcher) {
        coEvery { spotService.spot("1") } returns
            fixture().copy(likeCount = 7L, isLiked = false, isLikeable = true)
        coEvery { authService.isLoggedIn() } returns true

        val vm = vm()
        vm.load("1"); advanceUntilIdle()
        vm.toggleLike(); runCurrent()

        assertEquals(8, vm.likeCount.value)
        coVerify(exactly = 0) { likeService.add(any()) }
    }

    /** 서버가 갱신된 수를 돌려주므로 낙관적 +1 대신 그 값이 최종이다. */
    @Test
    fun `likeCount takes the count returned by the server`() = runTest(testDispatcher) {
        coEvery { spotService.spot("1") } returns
            fixture().copy(likeCount = 7L, isLiked = false, isLikeable = true)
        coEvery { authService.isLoggedIn() } returns true
        // 다른 사용자의 추천이 겹쳐 서버 값이 +1 보다 클 수 있다.
        coEvery { likeService.add("1") } returns 12L

        val vm = vm()
        vm.load("1"); advanceUntilIdle()
        vm.toggleLike(); advanceUntilIdle()

        assertEquals(12, vm.likeCount.value)
    }

    @Test
    fun `likeCount rolls back with liked when the request fails`() = runTest(testDispatcher) {
        coEvery { spotService.spot("1") } returns
            fixture().copy(likeCount = 7L, isLiked = false, isLikeable = true)
        coEvery { authService.isLoggedIn() } returns true
        coEvery { likeService.add("1") } throws RuntimeException("net")

        val vm = vm()
        vm.load("1"); advanceUntilIdle()
        vm.toggleLike(); advanceUntilIdle()

        assertFalse(vm.liked.value)
        assertEquals(7, vm.likeCount.value)
    }

    @Test
    fun `likeCount decrements when the recommendation is withdrawn`() = runTest(testDispatcher) {
        coEvery { spotService.spot("1") } returns
            fixture().copy(likeCount = 7L, isLiked = true, isLikeable = true)
        coEvery { authService.isLoggedIn() } returns true
        coEvery { likeService.remove("1") } returns 6L

        val vm = vm()
        vm.load("1"); advanceUntilIdle()
        vm.toggleLike(); advanceUntilIdle()

        assertEquals(6, vm.likeCount.value)
    }

    /** 재조회 응답이 이미 추천 반영된 수를 주므로, 이전 동기화 값이 남아 이중 계산되면 안 된다. */
    @Test
    fun `reload does not double count an already synced like`() = runTest(testDispatcher) {
        coEvery { spotService.spot("1") } returns
            fixture().copy(likeCount = 7L, isLiked = false, isLikeable = true)
        coEvery { authService.isLoggedIn() } returns true
        coEvery { likeService.add("1") } returns 8L

        val vm = vm()
        vm.load("1"); advanceUntilIdle()
        vm.toggleLike(); advanceUntilIdle()
        assertEquals(8, vm.likeCount.value)

        // 서버가 갱신된 상세를 준다 — 추천 여부/수가 모두 반영된 상태.
        coEvery { spotService.spot("1") } returns
            fixture().copy(likeCount = 8L, isLiked = true, isLikeable = true)
        vm.load("1"); advanceUntilIdle()

        assertEquals(8, vm.likeCount.value)
    }

    @Test
    fun `toggleLike likes optimistically, calls the service and toasts`() = runTest(testDispatcher) {
        coEvery { spotService.spot("1") } returns fixture().copy(isLiked = false, isLikeable = true)
        coEvery { authService.isLoggedIn() } returns true
        coEvery { likeService.add("1") } returns 8L

        val vm = vm()
        vm.load("1"); advanceUntilIdle()
        vm.toggleLike(); advanceUntilIdle()

        assertTrue(vm.liked.value)
        assertEquals("이 스팟을 추천했어요.", vm.toast.value?.message)
        // PV-143 — 추천 토스트는 체크 아이콘을 달지 않는다.
        assertEquals(false, vm.toast.value?.hasCheckIcon)
        coVerify(exactly = 1) { likeService.add("1") }
    }

    @Test
    fun `toggleLike unlikes without a toast`() = runTest(testDispatcher) {
        coEvery { spotService.spot("1") } returns fixture().copy(isLiked = true, isLikeable = true)
        coEvery { authService.isLoggedIn() } returns true
        coEvery { likeService.remove("1") } returns 7L

        val vm = vm()
        vm.load("1"); advanceUntilIdle()
        vm.toggleLike(); advanceUntilIdle()

        assertFalse(vm.liked.value)
        assertNull(vm.toast.value)
        coVerify(exactly = 1) { likeService.remove("1") }
    }

    @Test
    fun `toggleLike rolls back and toasts on failure`() = runTest(testDispatcher) {
        coEvery { spotService.spot("1") } returns fixture().copy(isLiked = false, isLikeable = true)
        coEvery { authService.isLoggedIn() } returns true
        coEvery { likeService.add("1") } throws RuntimeException("net")

        val vm = vm()
        vm.load("1"); advanceUntilIdle()
        vm.toggleLike(); advanceUntilIdle()

        assertFalse(vm.liked.value)
        assertEquals("잠시 후 다시 시도해주세요.", vm.toast.value?.message)
        // PV-143 — 추천 토스트는 체크 아이콘을 달지 않는다.
        assertEquals(false, vm.toast.value?.hasCheckIcon)
    }

    @Test
    fun `rapid toggleLike sends only the final state and toasts once`() = runTest(testDispatcher) {
        coEvery { spotService.spot("1") } returns fixture().copy(isLiked = false, isLikeable = true)
        coEvery { authService.isLoggedIn() } returns true
        coEvery { likeService.add("1") } returns 9L

        val vm = vm()
        vm.load("1"); advanceUntilIdle()

        // 따다다닥 — 홀수 번이라 최종 상태는 "추천함".
        repeat(5) { vm.toggleLike() }
        advanceUntilIdle()

        assertTrue(vm.liked.value)
        assertEquals("이 스팟을 추천했어요.", vm.toast.value?.message)
        coVerify(exactly = 1) { likeService.add("1") }
        coVerify(exactly = 0) { likeService.remove(any()) }
    }

    @Test
    fun `rapid toggleLike back to the original state sends nothing`() = runTest(testDispatcher) {
        coEvery { spotService.spot("1") } returns fixture().copy(isLiked = false, isLikeable = true)
        coEvery { authService.isLoggedIn() } returns true

        val vm = vm()
        vm.load("1"); advanceUntilIdle()

        // 짝수 번이라 원래 상태로 되돌아온다 — 요청도 토스트도 없어야 한다.
        repeat(4) { vm.toggleLike() }
        advanceUntilIdle()

        assertFalse(vm.liked.value)
        assertNull(vm.toast.value)
        coVerify(exactly = 0) { likeService.add(any()) }
        coVerify(exactly = 0) { likeService.remove(any()) }
    }

    @Test
    fun `toggleLike when logged out shows login prompt without server call`() = runTest(testDispatcher) {
        coEvery { spotService.spot("1") } returns fixture().copy(isLiked = false, isLikeable = true)
        coEvery { authService.isLoggedIn() } returns false

        val vm = vm()
        vm.load("1"); advanceUntilIdle()
        vm.toggleLike(); advanceUntilIdle()

        assertTrue(vm.isLoginRequired.value)
        assertFalse(vm.liked.value)
        coVerify(exactly = 0) { likeService.add(any()) }
    }
}
