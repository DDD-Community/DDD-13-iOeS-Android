package com.pickflow.android.feature.spotlist

import app.cash.turbine.test
import com.pickflow.android.common.ui.LoadState
import com.pickflow.android.core.services.protocols.AuthService
import com.pickflow.android.core.services.protocols.BookmarkService
import com.pickflow.android.core.services.protocols.Spot
import com.pickflow.android.core.services.protocols.SpotListService
import com.pickflow.android.core.services.protocols.SpotPage
import com.pickflow.android.core.services.protocols.SpotSort
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
    fun `refresh loads page 0 and emits Loaded`() = runTest(testDispatcher) {
        coEvery {
            listService.fetch(theme = null, page = 0, coordinates = null, sort = SpotSort.RECOMMENDED)
        } returns SpotPage(items = listOf(spot("a"), spot("b")), page = 0, hasNext = true)

        val vm = viewModel()
        vm.refresh()
        advanceUntilIdle()
        val state = vm.spots.value
        assertTrue(state is LoadState.Loaded && state.value.map { it.id } == listOf("a", "b"))
    }

    @Test
    fun `loadNextPage appends and stops when hasNext is false`() = runTest(testDispatcher) {
        coEvery {
            listService.fetch(theme = null, page = 0, coordinates = null, sort = SpotSort.RECOMMENDED)
        } returns SpotPage(items = listOf(spot("a")), page = 0, hasNext = true)
        coEvery {
            listService.fetch(theme = null, page = 1, coordinates = null, sort = SpotSort.RECOMMENDED)
        } returns SpotPage(items = listOf(spot("b")), page = 1, hasNext = false)

        val vm = viewModel()
        vm.refresh(); advanceUntilIdle()
        vm.loadNextPage(); advanceUntilIdle()
        val loaded = vm.spots.value as LoadState.Loaded
        assertEquals(2, loaded.value.size)

        // 추가 호출은 호출되지 않아야 함 (hasNext=false)
        vm.loadNextPage(); advanceUntilIdle()
        assertEquals(2, (vm.spots.value as LoadState.Loaded).value.size)
    }

    @Test
    fun `selectTheme resets page and refilters`() = runTest(testDispatcher) {
        coEvery {
            listService.fetch(theme = null, page = 0, coordinates = null, sort = SpotSort.RECOMMENDED)
        } returns SpotPage(items = listOf(spot("a", SpotTheme.SUNSET)), page = 0, hasNext = false)
        coEvery {
            listService.fetch(theme = SpotTheme.YUNSEUL, page = 0, coordinates = null, sort = SpotSort.RECOMMENDED)
        } returns SpotPage(items = listOf(spot("b", SpotTheme.YUNSEUL)), page = 0, hasNext = false)

        val vm = viewModel()
        vm.refresh(); advanceUntilIdle()
        vm.selectTheme(SpotTheme.YUNSEUL); advanceUntilIdle()
        val loaded = vm.spots.value as LoadState.Loaded
        assertEquals(listOf("b"), loaded.value.map { it.id })
    }

    @Test
    fun `selectSort resets page and re-fetches with new sort`() = runTest(testDispatcher) {
        coEvery {
            listService.fetch(theme = null, page = 0, coordinates = null, sort = SpotSort.RECOMMENDED)
        } returns SpotPage(items = listOf(spot("r")), page = 0, hasNext = false)
        coEvery {
            listService.fetch(theme = null, page = 0, coordinates = null, sort = SpotSort.DISTANCE)
        } returns SpotPage(items = listOf(spot("d")), page = 0, hasNext = false)

        val vm = viewModel()
        vm.refresh(); advanceUntilIdle()
        vm.selectSort(SpotSort.DISTANCE); advanceUntilIdle()
        assertEquals(SpotSort.DISTANCE, vm.sort.value)
        assertEquals(listOf("d"), (vm.spots.value as LoadState.Loaded).value.map { it.id })
    }

    @Test
    fun `empty result emits Empty`() = runTest(testDispatcher) {
        coEvery {
            listService.fetch(theme = null, page = 0, coordinates = null, sort = SpotSort.RECOMMENDED)
        } returns SpotPage(items = emptyList(), page = 0, hasNext = false)
        val vm = viewModel()
        vm.refresh(); advanceUntilIdle()
        assertEquals(LoadState.Empty, vm.spots.value)
    }

    @Test
    fun `failure emits Failed`() = runTest(testDispatcher) {
        val boom = RuntimeException("nope")
        coEvery { listService.fetch(any(), any(), any(), any()) } throws boom
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
