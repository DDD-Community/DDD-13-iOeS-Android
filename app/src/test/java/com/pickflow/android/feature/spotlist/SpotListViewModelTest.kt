package com.pickflow.android.feature.spotlist

import app.cash.turbine.test
import com.pickflow.android.common.ui.LoadState
import com.pickflow.android.core.services.protocols.AuthService
import com.pickflow.android.core.services.protocols.BookmarkService
import com.pickflow.android.core.services.protocols.Spot
import com.pickflow.android.core.services.protocols.SpotListService
import com.pickflow.android.core.services.protocols.SpotPage
import com.pickflow.android.core.services.protocols.SpotTheme
import io.mockk.coEvery
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
class SpotListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var listService: SpotListService
    private lateinit var bookmarkService: BookmarkService
    private lateinit var authService: AuthService

    private fun spot(id: String, theme: SpotTheme = SpotTheme.SUNSET) =
        Spot(id = id, name = id, theme = theme, latitude = 0.0, longitude = 0.0)

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        listService = mockk()
        bookmarkService = mockk(relaxed = true)
        authService = mockk(relaxed = true)
        coEvery { authService.isLoggedIn() } returns true
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = SpotListViewModel(listService, bookmarkService, authService)

    @Test
    fun `refresh loads first page and emits Loaded`() = runTest(testDispatcher) {
        coEvery { listService.fetch(null, null, any()) } returns
            SpotPage(listOf(spot("a"), spot("b")), nextCursor = "2")

        val vm = viewModel()
        vm.refresh()
        advanceUntilIdle()
        val state = vm.spots.value
        // 기본 정렬 LATEST = id 내림차순
        assertTrue(state is LoadState.Loaded && state.value.map { it.id } == listOf("b", "a"))
    }

    @Test
    fun `loadNextPage appends results and stops at null cursor`() = runTest(testDispatcher) {
        coEvery { listService.fetch(null, null, any()) } returns
            SpotPage(listOf(spot("a")), nextCursor = "1")
        coEvery { listService.fetch(null, "1", any()) } returns
            SpotPage(listOf(spot("b")), nextCursor = null)

        val vm = viewModel()
        vm.refresh(); advanceUntilIdle()
        vm.loadNextPage(); advanceUntilIdle()

        val loaded = vm.spots.value as LoadState.Loaded
        assertEquals(2, loaded.value.size)

        vm.loadNextPage(); advanceUntilIdle()
        val loaded2 = vm.spots.value as LoadState.Loaded
        assertEquals(2, loaded2.value.size)
    }

    @Test
    fun `selectTheme resets cursor and refilters`() = runTest(testDispatcher) {
        coEvery { listService.fetch(null, null, any()) } returns
            SpotPage(listOf(spot("a", SpotTheme.SUNSET)), nextCursor = null)
        coEvery { listService.fetch(SpotTheme.YUNSEUL, null, any()) } returns
            SpotPage(listOf(spot("b", SpotTheme.YUNSEUL)), nextCursor = null)

        val vm = viewModel()
        vm.refresh(); advanceUntilIdle()
        vm.selectTheme(SpotTheme.YUNSEUL); advanceUntilIdle()

        val loaded = vm.spots.value as LoadState.Loaded
        assertEquals(listOf("b"), loaded.value.map { it.id })
    }

    @Test
    fun `selectSort reorders accumulated spots`() = runTest(testDispatcher) {
        coEvery { listService.fetch(null, null, any()) } returns
            SpotPage(listOf(spot("a"), spot("c"), spot("b")), nextCursor = null)

        val vm = viewModel()
        vm.refresh(); advanceUntilIdle()
        vm.selectSort(SpotSort.POPULAR) // name 오름차순(name == id)
        val loaded = vm.spots.value as LoadState.Loaded
        assertEquals(listOf("a", "b", "c"), loaded.value.map { it.id })
        assertEquals(SpotSort.POPULAR, vm.sort.value)
    }

    @Test
    fun `empty result emits Empty`() = runTest(testDispatcher) {
        coEvery { listService.fetch(null, null, any()) } returns SpotPage(emptyList(), null)
        val vm = viewModel()
        vm.refresh(); advanceUntilIdle()
        assertEquals(LoadState.Empty, vm.spots.value)
    }

    @Test
    fun `failure emits Failed`() = runTest(testDispatcher) {
        val boom = RuntimeException("nope")
        coEvery { listService.fetch(any(), any(), any()) } throws boom
        val vm = viewModel()
        vm.refresh(); advanceUntilIdle()
        val s = vm.spots.value
        assertTrue(s is LoadState.Failed && s.error === boom)
    }

    @Test
    fun `toggleBookmark refreshes bookmarkedIds when logged in`() = runTest(testDispatcher) {
        coEvery { authService.isLoggedIn() } returns true
        coEvery { bookmarkService.toggle("a") } returns true
        coEvery { bookmarkService.bookmarkedIds() } returns setOf("a")
        val vm = viewModel()
        vm.bookmarkedIds.test {
            assertEquals(emptySet<String>(), awaitItem())
            vm.toggleBookmark("a"); advanceUntilIdle()
            assertEquals(setOf("a"), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `toggleBookmark shows login prompt when not logged in`() = runTest(testDispatcher) {
        coEvery { authService.isLoggedIn() } returns false
        val vm = viewModel()
        assertFalse(vm.showLoginPrompt.value)
        vm.toggleBookmark("a"); advanceUntilIdle()
        assertTrue(vm.showLoginPrompt.value)
        vm.dismissLoginPrompt()
        assertFalse(vm.showLoginPrompt.value)
    }
}
