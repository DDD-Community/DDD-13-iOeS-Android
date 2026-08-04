package com.pickflow.android.feature.spotlist

import com.pickflow.android.common.ui.LoadState
import com.pickflow.android.core.services.protocols.AuthService
import com.pickflow.android.core.services.protocols.BookmarkService
import com.pickflow.android.core.services.protocols.LocationService
import com.pickflow.android.core.services.protocols.Spot
import com.pickflow.android.core.services.protocols.SpotListService
import com.pickflow.android.core.services.protocols.SpotPage
import com.pickflow.android.core.services.protocols.SpotSort
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
class SpotListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var listService: SpotListService
    private lateinit var bookmarkService: BookmarkService
    private lateinit var authService: AuthService
    private lateinit var locationService: LocationService

    private fun spot(id: String, theme: SpotTheme = SpotTheme.SUNSET) =
        Spot(id = id, name = id, theme = theme, latitude = 0.0, longitude = 0.0)

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        listService = mockk()
        bookmarkService = mockk(relaxed = true)
        authService = mockk(relaxed = true)
        locationService = mockk(relaxed = true)
        coEvery { authService.isLoggedIn() } returns true
        coEvery { locationService.currentLocation() } returns null
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() =
        SpotListViewModel(listService, bookmarkService, authService, locationService)

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

        vm.loadNextPage(); advanceUntilIdle()
        assertEquals(2, (vm.spots.value as LoadState.Loaded).value.size)
    }

    @Test
    fun `overlapping ids across pages are deduped to keep grid keys unique`() = runTest(testDispatcher) {
        // 서버가 페이지 경계에서 같은 스팟(b)을 겹쳐 내려도 중복 없이 누적돼야 한다.
        // (LazyVerticalStaggeredGrid 의 key={it.id} 중복 → IllegalArgumentException 크래시 방지)
        coEvery {
            listService.fetch(theme = null, page = 0, coordinates = null, sort = SpotSort.RECOMMENDED)
        } returns SpotPage(items = listOf(spot("a"), spot("b")), page = 0, hasNext = true)
        coEvery {
            listService.fetch(theme = null, page = 1, coordinates = null, sort = SpotSort.RECOMMENDED)
        } returns SpotPage(items = listOf(spot("b"), spot("c")), page = 1, hasNext = false)

        val vm = viewModel()
        vm.refresh(); advanceUntilIdle()
        vm.loadNextPage(); advanceUntilIdle()

        val ids = (vm.spots.value as LoadState.Loaded).value.map { it.id }
        assertEquals(listOf("a", "b", "c"), ids)
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `concurrent loadNextPage during in-flight load fires only one fetch`() = runTest(testDispatcher) {
        coEvery {
            listService.fetch(theme = null, page = 0, coordinates = null, sort = SpotSort.RECOMMENDED)
        } returns SpotPage(items = listOf(spot("a")), page = 0, hasNext = true)
        coEvery {
            listService.fetch(theme = null, page = 1, coordinates = null, sort = SpotSort.RECOMMENDED)
        } returns SpotPage(items = listOf(spot("b")), page = 1, hasNext = true)

        val vm = viewModel()
        vm.refresh(); advanceUntilIdle()

        // 빠른 스크롤로 loadNextPage 가 연타되는 상황 — 첫 호출이 in-flight 인 동안 둘째는 무시돼야 한다.
        vm.loadNextPage()
        vm.loadNextPage()
        advanceUntilIdle()

        coVerify(exactly = 1) {
            listService.fetch(theme = null, page = 1, coordinates = null, sort = SpotSort.RECOMMENDED)
        }
        assertEquals(listOf("a", "b"), (vm.spots.value as LoadState.Loaded).value.map { it.id })
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
        } returns SpotPage(items = listOf(spot("d")), page = 0, hasNext = false)
        coEvery {
            listService.fetch(theme = null, page = 0, coordinates = null, sort = SpotSort.DISTANCE)
        } returns SpotPage(items = listOf(spot("b")), page = 0, hasNext = false)

        val vm = viewModel()
        vm.refresh(); advanceUntilIdle()
        vm.selectSort(SpotSort.DISTANCE); advanceUntilIdle()
        assertEquals(SpotSort.DISTANCE, vm.sort.value)
        assertEquals(listOf("b"), (vm.spots.value as LoadState.Loaded).value.map { it.id })
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
        coEvery { bookmarkService.toggle("a") } returns true
        coEvery { bookmarkService.bookmarkedIds() } returns setOf("a")
        val vm = viewModel()
        vm.toggleBookmark("a"); advanceUntilIdle()
        assertEquals(setOf("a"), vm.bookmarkedIds.value)
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
