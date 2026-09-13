package com.pickflow.android.feature.archive

import app.cash.turbine.test
import com.pickflow.android.common.ui.LoadState
import com.pickflow.android.core.services.protocols.Archive
import com.pickflow.android.core.services.protocols.ArchiveService
import com.pickflow.android.core.services.protocols.AuthService
import com.pickflow.android.core.services.protocols.BookmarkService
import com.pickflow.android.core.services.protocols.Coordinates
import com.pickflow.android.core.services.protocols.ImagePayload
import com.pickflow.android.core.services.protocols.LocationService
import com.pickflow.android.core.services.protocols.MySpot
import com.pickflow.android.core.services.protocols.MySpotPage
import com.pickflow.android.core.services.protocols.MySpotService
import com.pickflow.android.core.services.protocols.MySpotStatus
import com.pickflow.android.core.services.protocols.SavedSpot
import com.pickflow.android.core.services.protocols.SavedSpotAvailability
import com.pickflow.android.core.services.protocols.SavedSpotPage
import com.pickflow.android.core.services.protocols.SpotTheme
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ArchiveViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var archiveService: ArchiveService
    private lateinit var bookmarkService: BookmarkService
    private lateinit var authService: AuthService
    private lateinit var locationService: LocationService
    private lateinit var mySpotService: MySpotService

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        archiveService = mockk(relaxed = true)
        bookmarkService = mockk(relaxed = true)
        authService = mockk(relaxed = true)
        locationService = mockk(relaxed = true)
        mySpotService = mockk(relaxed = true)
        coEvery { locationService.currentLocation() } returns null
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun vm() = ArchiveViewModel(
        archiveService, bookmarkService, authService, locationService, mySpotService,
    )

    private fun mySpot(id: Long, status: MySpotStatus = MySpotStatus.PUBLISHED) = MySpot(
        id = id,
        name = "my$id",
        theme = SpotTheme.SUNSET,
        imageUrl = null,
        latitude = 0.0,
        longitude = 0.0,
        distanceKm = null,
        createdAt = "2026-01-01T00:00:00Z",
        status = status,
        bookmarkCount = 0,
    )

    private fun savedSpot(
        id: Long,
        name: String = "spot$id",
        availability: SavedSpotAvailability = SavedSpotAvailability.AVAILABLE,
    ) = SavedSpot(
        id = id,
        name = name,
        theme = SpotTheme.SUNSET,
        imageUrl = null,
        latitude = 0.0,
        longitude = 0.0,
        distanceKm = null,
        savedAt = "2026-01-01T00:00:00Z",
        deleted = false,
        availability = availability,
        isUserGenerated = availability == SavedSpotAvailability.AUTHOR_PRIVATE,
    )

    @Test
    fun `onAppear emits SignedOut when not logged in`() = runTest(testDispatcher) {
        coEvery { authService.isLoggedIn() } returns false
        val viewModel = vm()
        viewModel.onAppear()
        advanceUntilIdle()
        assertEquals(ArchiveLoadState.SignedOut, viewModel.state.value)
    }

    @Test
    fun `onAppear emits Loaded with saved spots`() = runTest(testDispatcher) {
        coEvery { authService.isLoggedIn() } returns true
        coEvery { archiveService.fetch() } returns Archive(name = "내 보관함", imageUrl = "https://x/y.jpg")
        coEvery { bookmarkService.savedSpots(0, null) } returns SavedSpotPage(
            items = listOf(savedSpot(1), savedSpot(2)),
            page = 0,
            hasNext = true,
        )
        val viewModel = vm()
        viewModel.onAppear()
        advanceUntilIdle()

        val s = viewModel.state.value
        assertTrue(s is ArchiveLoadState.Loaded && s.items.size == 2 && s.hasNext)
        assertEquals("내 보관함", viewModel.archiveName.value)
        assertEquals("https://x/y.jpg", viewModel.archiveImageUrl.value)
    }

    @Test
    fun `onAppear emits Empty for zero saved spots`() = runTest(testDispatcher) {
        coEvery { authService.isLoggedIn() } returns true
        coEvery { bookmarkService.savedSpots(0, null) } returns SavedSpotPage(emptyList(), 0, false)
        val viewModel = vm()
        viewModel.onAppear()
        advanceUntilIdle()
        assertEquals(ArchiveLoadState.Empty, viewModel.state.value)
    }

    @Test
    fun `onAppear emits Failed on bookmark service error`() = runTest(testDispatcher) {
        coEvery { authService.isLoggedIn() } returns true
        coEvery { bookmarkService.savedSpots(any(), any()) } throws RuntimeException("boom")
        val viewModel = vm()
        viewModel.onAppear()
        advanceUntilIdle()
        val s = viewModel.state.value
        assertTrue(s is ArchiveLoadState.Failed && s.message == "boom")
    }

    @Test
    fun `tabChanged updates selectedTab`() {
        val viewModel = vm()
        assertEquals(ArchiveTab.SavedSpots, viewModel.selectedTab.value)
        viewModel.tabChanged(ArchiveTab.MySpots)
        assertEquals(ArchiveTab.MySpots, viewModel.selectedTab.value)
    }

    @Test
    fun `renameArchive trims to 15 chars and updates name on success`() = runTest(testDispatcher) {
        coEvery { archiveService.updateName(any()) } answers { Archive(name = firstArg(), imageUrl = null) }
        val viewModel = vm()
        viewModel.renameArchive("0123456789ABCDEFGHIJ") // 20자
        advanceUntilIdle()
        assertEquals("0123456789ABCDE", viewModel.archiveName.value) // 15자
    }

    @Test
    fun `renameArchive rolls back and toasts on failure`() = runTest(testDispatcher) {
        coEvery { archiveService.updateName(any()) } throws RuntimeException("nope")
        val viewModel = vm()
        // toast 는 2초 후 자동 reset 되므로 advanceUntilIdle 대신 emission 캡처.
        viewModel.toast.test {
            assertEquals(null, awaitItem())
            viewModel.renameArchive("새이름")
            runCurrent()
            assertEquals("이름 변경에 실패했어요.", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals("나의 보관함", viewModel.archiveName.value)
    }

    @Test
    fun `updateCoverImage updates bytes and url on success`() = runTest(testDispatcher) {
        val payload = ImagePayload(bytes = byteArrayOf(1, 2, 3), mimeType = "image/jpeg", filename = "x.jpg")
        coEvery { archiveService.updateImage(payload) } returns Archive(name = "보관함", imageUrl = "https://new")
        val viewModel = vm()
        viewModel.toast.test {
            assertEquals(null, awaitItem())
            viewModel.updateCoverImage(payload)
            runCurrent()
            assertEquals("커버 이미지가 변경되었습니다.", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals("https://new", viewModel.archiveImageUrl.value)
        assertEquals("보관함", viewModel.archiveName.value)
    }

    @Test
    fun `updateCoverImage clears bytes and toasts on failure`() = runTest(testDispatcher) {
        val payload = ImagePayload(bytes = byteArrayOf(1), mimeType = "image/jpeg", filename = "x.jpg")
        coEvery { archiveService.updateImage(any()) } throws RuntimeException("upload failed")
        val viewModel = vm()
        viewModel.toast.test {
            assertEquals(null, awaitItem())
            viewModel.updateCoverImage(payload)
            runCurrent()
            assertEquals("이미지 업로드에 실패했어요.", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(null, viewModel.coverImageBytes.value)
    }

    @Test
    fun `bookmarkTapped removes item optimistically and calls service`() = runTest(testDispatcher) {
        coEvery { authService.isLoggedIn() } returns true
        coEvery { bookmarkService.savedSpots(0, null) } returns SavedSpotPage(
            items = listOf(savedSpot(1), savedSpot(2)),
            page = 0,
            hasNext = false,
        )
        coEvery { bookmarkService.remove("1") } returns 0L
        val viewModel = vm()
        viewModel.onAppear(); advanceUntilIdle()

        viewModel.bookmarkTapped(1L); advanceUntilIdle()

        val loaded = viewModel.state.value as ArchiveLoadState.Loaded
        assertEquals(listOf(2L), loaded.items.map { it.id })
        coVerify { bookmarkService.remove("1") }
    }

    @Test
    fun `bookmarkTapped becomes Empty when last item removed`() = runTest(testDispatcher) {
        coEvery { authService.isLoggedIn() } returns true
        coEvery { bookmarkService.savedSpots(0, null) } returns SavedSpotPage(
            items = listOf(savedSpot(1)), page = 0, hasNext = false,
        )
        coEvery { bookmarkService.remove("1") } returns 0L
        val viewModel = vm()
        viewModel.onAppear(); advanceUntilIdle()
        viewModel.bookmarkTapped(1L); advanceUntilIdle()
        assertEquals(ArchiveLoadState.Empty, viewModel.state.value)
    }

    @Test
    fun `bookmarkTapped rolls back on service failure`() = runTest(testDispatcher) {
        coEvery { authService.isLoggedIn() } returns true
        coEvery { bookmarkService.savedSpots(0, null) } returns SavedSpotPage(
            items = listOf(savedSpot(1), savedSpot(2)), page = 0, hasNext = false,
        )
        coEvery { bookmarkService.remove("1") } throws RuntimeException("net")
        val viewModel = vm()
        viewModel.onAppear(); advanceUntilIdle()

        viewModel.toast.test {
            assertEquals(null, awaitItem())
            viewModel.bookmarkTapped(1L)
            runCurrent()
            assertEquals("북마크 해제에 실패했어요.", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        val loaded = viewModel.state.value as ArchiveLoadState.Loaded
        assertEquals(listOf(1L, 2L), loaded.items.map { it.id })
    }

    @Test
    fun `author private bookmark removal failure restores its original index`() = runTest(testDispatcher) {
        val authorPrivate = savedSpot(
            id = 2L,
            availability = SavedSpotAvailability.AUTHOR_PRIVATE,
        )
        coEvery { authService.isLoggedIn() } returns true
        coEvery { bookmarkService.savedSpots(0, null) } returns SavedSpotPage(
            items = listOf(savedSpot(1), authorPrivate, savedSpot(3)),
            page = 0,
            hasNext = true,
        )
        coEvery { bookmarkService.remove("2") } throws RuntimeException("network")
        val viewModel = vm()
        viewModel.onAppear()
        advanceUntilIdle()

        viewModel.bookmarkTapped(2L)
        runCurrent()

        val loaded = viewModel.state.value as ArchiveLoadState.Loaded
        assertEquals(listOf(1L, 2L, 3L), loaded.items.map { it.id })
        assertEquals(SavedSpotAvailability.AUTHOR_PRIVATE, loaded.items[1].availability)
        assertTrue(loaded.hasNext)
    }

    @Test
    fun `loadNextPageIfNeeded appends next page when trigger item appears`() = runTest(testDispatcher) {
        coEvery { authService.isLoggedIn() } returns true
        val firstPage = (1L..5L).map { savedSpot(it) }
        val secondPage = (6L..8L).map { savedSpot(it) }
        coEvery { bookmarkService.savedSpots(0, null) } returns SavedSpotPage(firstPage, 0, true)
        coEvery { bookmarkService.savedSpots(1, null) } returns SavedSpotPage(secondPage, 1, false)

        val viewModel = vm()
        viewModel.onAppear(); advanceUntilIdle()

        // trigger item = index >= size-3 = 2. id=3 corresponds to index 2.
        viewModel.loadNextPageIfNeeded(firstPage[2]); advanceUntilIdle()

        val loaded = viewModel.state.value as ArchiveLoadState.Loaded
        assertEquals(8, loaded.items.size)
        assertEquals(false, loaded.hasNext)
    }

    @Test
    fun `loadNextPageIfNeeded ignores when item is too early`() = runTest(testDispatcher) {
        coEvery { authService.isLoggedIn() } returns true
        val firstPage = (1L..5L).map { savedSpot(it) }
        coEvery { bookmarkService.savedSpots(0, null) } returns SavedSpotPage(firstPage, 0, true)
        val viewModel = vm()
        viewModel.onAppear(); advanceUntilIdle()

        viewModel.loadNextPageIfNeeded(firstPage[0]); advanceUntilIdle()
        coVerify(exactly = 0) { bookmarkService.savedSpots(1, any()) }
    }

    // MARK: - MySpots tab

    @Test
    fun `tabChanged to MySpots refetches on every entry, silently after the first`() =
        runTest(testDispatcher) {
            coEvery { mySpotService.list(0, null) } returns MySpotPage(
                items = listOf(mySpot(1), mySpot(2, MySpotStatus.PENDING)),
                page = 0,
                hasNext = false,
            )
            val viewModel = vm()

            viewModel.tabChanged(ArchiveTab.MySpots); advanceUntilIdle()
            val s = viewModel.mySpots.value
            assertTrue(s is LoadState.Loaded && s.value.map { it.id } == listOf(1L, 2L))

            // 남이 스팟을 내린 건 알림으로 알 수 없다 — 탭을 누를 때마다 다시 읽는다.
            viewModel.tabChanged(ArchiveTab.SavedSpots)
            viewModel.tabChanged(ArchiveTab.MySpots)
            // 재진입은 Loading 을 거치지 않는다 — 이전 목록이 그대로 떠 있다(깜빡임 없음).
            assertTrue(viewModel.mySpots.value is LoadState.Loaded)
            advanceUntilIdle()
            coVerify(exactly = 2) { mySpotService.list(0, null) }
        }

    @Test
    fun `onAppear refreshes the visible tab after deletion and restarts pagination`() = runTest(testDispatcher) {
        coEvery { authService.isLoggedIn() } returns true
        coEvery { bookmarkService.savedSpots(0, null) } returns SavedSpotPage(listOf(savedSpot(1)), 0, false)
        coEvery { mySpotService.list(0, null) } returns MySpotPage(listOf(mySpot(1)), 0, true)
        coEvery { mySpotService.list(1, null) } returns MySpotPage(listOf(mySpot(2)), 1, false)
        val viewModel = vm()
        viewModel.onAppear()
        advanceUntilIdle()
        viewModel.tabChanged(ArchiveTab.MySpots)
        advanceUntilIdle()
        viewModel.loadNextMySpotPageIfNeeded(mySpot(1))
        advanceUntilIdle()

        coEvery { bookmarkService.savedSpots(0, null) } returns SavedSpotPage(emptyList(), 0, false)
        coEvery { mySpotService.list(0, null) } returns MySpotPage(listOf(mySpot(2)), 0, true)
        coEvery { mySpotService.list(1, null) } returns MySpotPage(listOf(mySpot(3)), 1, false)
        viewModel.onAppear()
        advanceUntilIdle()

        // 보이는 탭(MySpots)만 갱신되고 페이지네이션은 1페이지로 되감긴다.
        assertEquals(ArchiveTab.MySpots, viewModel.selectedTab.value)
        assertEquals(LoadState.Loaded(listOf(mySpot(2))), viewModel.mySpots.value)
        viewModel.loadNextMySpotPageIfNeeded(mySpot(2))
        advanceUntilIdle()
        assertEquals(LoadState.Loaded(listOf(mySpot(2), mySpot(3))), viewModel.mySpots.value)
        coVerify(exactly = 2) { mySpotService.list(1, null) }

        // 안 보이던 저장 탭은 미리 읽지 않는다 — 그 탭을 열 때 갱신된다.
        viewModel.tabChanged(ArchiveTab.SavedSpots)
        advanceUntilIdle()
        assertEquals(ArchiveLoadState.Empty, viewModel.state.value)
    }

    @Test
    fun `MySpots cache is refreshed when the tab is reopened`() = runTest(testDispatcher) {
        coEvery { authService.isLoggedIn() } returns true
        coEvery { mySpotService.list(0, null) } returns MySpotPage(listOf(mySpot(1)), 0, false)
        val viewModel = vm()
        viewModel.tabChanged(ArchiveTab.MySpots)
        advanceUntilIdle()
        viewModel.tabChanged(ArchiveTab.SavedSpots)

        coEvery { mySpotService.list(0, null) } returns MySpotPage(emptyList(), 0, false)
        viewModel.onAppear()
        advanceUntilIdle()
        viewModel.tabChanged(ArchiveTab.MySpots)
        advanceUntilIdle()

        assertEquals(LoadState.Empty, viewModel.mySpots.value)
        assertEquals(false, viewModel.isLoadingNextPage.value)
    }

    @Test
    fun `onAppear keeps unvisited MySpots lazy`() = runTest(testDispatcher) {
        coEvery { authService.isLoggedIn() } returns true
        val viewModel = vm()
        viewModel.onAppear()
        advanceUntilIdle()

        assertEquals(LoadState.Idle, viewModel.mySpots.value)
        coVerify(exactly = 0) { mySpotService.list(any(), any()) }
    }

    @Test
    fun `failed refresh replaces stale MySpots and can recover on next appearance`() = runTest(testDispatcher) {
        coEvery { authService.isLoggedIn() } returns true
        coEvery { mySpotService.list(0, null) } returns MySpotPage(listOf(mySpot(1)), 0, false)
        val viewModel = vm()
        viewModel.tabChanged(ArchiveTab.MySpots)
        advanceUntilIdle()

        val error = RuntimeException("offline")
        coEvery { mySpotService.list(0, null) } throws error
        viewModel.onAppear()
        advanceUntilIdle()
        assertEquals(LoadState.Failed(error), viewModel.mySpots.value)

        coEvery { mySpotService.list(0, null) } returns MySpotPage(emptyList(), 0, false)
        viewModel.onAppear()
        advanceUntilIdle()
        assertEquals(LoadState.Empty, viewModel.mySpots.value)
    }

    @Test
    fun `tab spam drops the stale response instead of letting it overwrite the newest`() =
        runTest(testDispatcher) {
            coEvery { authService.isLoggedIn() } returns true
            // 첫 조회는 응답이 늦다. 두 번째 조회가 먼저 끝난 뒤에야 도착한다.
            val slow = CompletableDeferred<SavedSpotPage>()
            coEvery { bookmarkService.savedSpots(0, null) } coAnswers { slow.await() }
            val viewModel = vm()

            viewModel.tabChanged(ArchiveTab.SavedSpots)
            runCurrent()

            // 연타 — 두 번째 조회가 직전 조회를 취소한다.
            coEvery { bookmarkService.savedSpots(0, null) } returns
                SavedSpotPage(listOf(savedSpot(2)), 0, false)
            viewModel.tabChanged(ArchiveTab.SavedSpots)
            advanceUntilIdle()

            // 늦게 도착한 첫 응답은 버려진다 — 최신 결과를 덮지 않는다.
            slow.complete(SavedSpotPage(listOf(savedSpot(1)), 0, false))
            advanceUntilIdle()

            val state = viewModel.state.value
            assertTrue(state is ArchiveLoadState.Loaded && state.items.map { it.id } == listOf(2L))
            coVerify(exactly = 2) { bookmarkService.savedSpots(0, null) }
        }

    @Test
    fun `refresh cancels an old MySpots page so deleted items cannot reappear`() = runTest(testDispatcher) {
        coEvery { authService.isLoggedIn() } returns true
        coEvery { mySpotService.list(0, null) } returns MySpotPage(listOf(mySpot(1)), 0, true)
        val oldPage = CompletableDeferred<MySpotPage>()
        coEvery { mySpotService.list(1, null) } coAnswers { oldPage.await() }
        val viewModel = vm()
        viewModel.tabChanged(ArchiveTab.MySpots)
        advanceUntilIdle()
        viewModel.loadNextMySpotPageIfNeeded(mySpot(1))
        runCurrent()
        assertTrue(viewModel.isLoadingNextPage.value)

        coEvery { mySpotService.list(0, null) } returns MySpotPage(emptyList(), 0, false)
        viewModel.onAppear()
        runCurrent()
        oldPage.complete(MySpotPage(listOf(mySpot(1)), 1, false))
        advanceUntilIdle()

        assertEquals(LoadState.Empty, viewModel.mySpots.value)
        assertEquals(false, viewModel.isLoadingNextPage.value)
        assertEquals(null, viewModel.toast.value)
    }

    @Test
    fun `MySpots preserves all five server statuses without collapsing pending states`() =
        runTest(testDispatcher) {
            val statuses = listOf(
                MySpotStatus.DRAFT,
                MySpotStatus.PENDING,
                MySpotStatus.RE_REVIEW_PENDING,
                MySpotStatus.REJECTED,
                MySpotStatus.PUBLISHED,
            )
            coEvery { mySpotService.list(0, null) } returns MySpotPage(
                items = statuses.mapIndexed { index, status -> mySpot(index.toLong(), status) },
                page = 0,
                hasNext = false,
            )
            val viewModel = vm()

            viewModel.tabChanged(ArchiveTab.MySpots)
            advanceUntilIdle()

            val loaded = viewModel.mySpots.value as LoadState.Loaded
            assertEquals(statuses, loaded.value.map { it.status })
        }

    @Test
    fun `MySpots Empty state when service returns empty list`() = runTest(testDispatcher) {
        coEvery { mySpotService.list(0, null) } returns MySpotPage(emptyList(), 0, false)
        val viewModel = vm()
        viewModel.tabChanged(ArchiveTab.MySpots); advanceUntilIdle()
        assertEquals(LoadState.Empty, viewModel.mySpots.value)
    }

    @Test
    fun `MySpots Failed state on service error`() = runTest(testDispatcher) {
        val boom = RuntimeException("net")
        coEvery { mySpotService.list(any(), any()) } throws boom
        val viewModel = vm()
        viewModel.tabChanged(ArchiveTab.MySpots); advanceUntilIdle()
        val s = viewModel.mySpots.value
        assertTrue(s is LoadState.Failed && s.error === boom)
    }

    @Test
    fun `loadNextMySpotPageIfNeeded appends next page when trigger item appears`() = runTest(testDispatcher) {
        val firstPage = (1L..5L).map { mySpot(it) }
        val secondPage = (6L..8L).map { mySpot(it) }
        coEvery { mySpotService.list(0, null) } returns MySpotPage(firstPage, 0, true)
        coEvery { mySpotService.list(1, null) } returns MySpotPage(secondPage, 1, false)

        val viewModel = vm()
        viewModel.tabChanged(ArchiveTab.MySpots); advanceUntilIdle()
        viewModel.loadNextMySpotPageIfNeeded(firstPage[2]); advanceUntilIdle()

        val loaded = viewModel.mySpots.value as LoadState.Loaded
        assertEquals(8, loaded.value.size)
    }
}
