package com.pickflow.android.feature.archive

import com.pickflow.android.core.services.protocols.Archive
import com.pickflow.android.core.services.protocols.ArchiveService
import com.pickflow.android.core.services.protocols.AuthService
import com.pickflow.android.core.services.protocols.BookmarkService
import com.pickflow.android.core.services.protocols.Coordinates
import com.pickflow.android.core.services.protocols.ImagePayload
import com.pickflow.android.core.services.protocols.LocationService
import com.pickflow.android.core.services.protocols.SavedSpot
import com.pickflow.android.core.services.protocols.SavedSpotPage
import com.pickflow.android.core.services.protocols.SpotTheme
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
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
class ArchiveViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var archiveService: ArchiveService
    private lateinit var bookmarkService: BookmarkService
    private lateinit var authService: AuthService
    private lateinit var locationService: LocationService

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        archiveService = mockk(relaxed = true)
        bookmarkService = mockk(relaxed = true)
        authService = mockk(relaxed = true)
        locationService = mockk(relaxed = true)
        coEvery { locationService.currentLocation() } returns null
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun vm() = ArchiveViewModel(archiveService, bookmarkService, authService, locationService)

    private fun savedSpot(id: Long, name: String = "spot$id") = SavedSpot(
        id = id,
        name = name,
        theme = SpotTheme.SUNSET,
        imageUrl = null,
        latitude = 0.0,
        longitude = 0.0,
        distanceKm = null,
        savedAt = "2026-01-01T00:00:00Z",
        deleted = false,
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
        viewModel.renameArchive("새이름")
        advanceUntilIdle()
        assertEquals("나의 보관함", viewModel.archiveName.value)
        assertEquals("이름 변경에 실패했어요.", viewModel.toast.value)
    }

    @Test
    fun `updateCoverImage updates bytes and url on success`() = runTest(testDispatcher) {
        val payload = ImagePayload(bytes = byteArrayOf(1, 2, 3), mimeType = "image/jpeg", filename = "x.jpg")
        coEvery { archiveService.updateImage(payload) } returns Archive(name = "보관함", imageUrl = "https://new")
        val viewModel = vm()
        viewModel.updateCoverImage(payload)
        advanceUntilIdle()
        assertEquals("https://new", viewModel.archiveImageUrl.value)
        assertEquals("보관함", viewModel.archiveName.value)
        assertEquals("커버 이미지가 변경되었습니다.", viewModel.toast.value)
    }

    @Test
    fun `updateCoverImage clears bytes and toasts on failure`() = runTest(testDispatcher) {
        val payload = ImagePayload(bytes = byteArrayOf(1), mimeType = "image/jpeg", filename = "x.jpg")
        coEvery { archiveService.updateImage(any()) } throws RuntimeException("upload failed")
        val viewModel = vm()
        viewModel.updateCoverImage(payload)
        advanceUntilIdle()
        assertEquals(null, viewModel.coverImageBytes.value)
        assertEquals("이미지 업로드에 실패했어요.", viewModel.toast.value)
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
        viewModel.bookmarkTapped(1L); advanceUntilIdle()

        val loaded = viewModel.state.value as ArchiveLoadState.Loaded
        assertEquals(listOf(1L, 2L), loaded.items.map { it.id })
        assertEquals("북마크 해제에 실패했어요.", viewModel.toast.value)
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
}
