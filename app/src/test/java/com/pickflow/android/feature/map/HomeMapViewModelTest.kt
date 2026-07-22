package com.pickflow.android.feature.map

import com.pickflow.android.common.ui.LoadState
import com.pickflow.android.core.services.protocols.AuthService
import com.pickflow.android.core.services.protocols.BookmarkService
import com.pickflow.android.core.services.protocols.ExternalAppLauncher
import com.pickflow.android.core.services.protocols.LocationService
import com.pickflow.android.core.services.protocols.Spot
import com.pickflow.android.core.services.protocols.SpotListService
import com.pickflow.android.core.services.protocols.SpotMapService
import com.pickflow.android.core.services.protocols.SpotPage
import com.pickflow.android.core.services.protocols.SpotService
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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * HomeMapViewModel — `clusters` StateFlow 가 `curationSpots` 로 교체된 후의 단위 테스트.
 *
 * viewport partition 시나리오는 [HomeMapViewportPartitionTest] 에 위임. 본 테스트는
 * `load()` / `setZoom` / `selectMood` / `selectCluster` / `selectMapListMode` 흐름만 검증.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeMapViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var spotListService: SpotListService
    private lateinit var spotMapService: SpotMapService
    private lateinit var locationService: LocationService

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        spotListService = mockk()
        spotMapService = mockk(relaxed = true)
        locationService = mockk(relaxed = true)
    }

    @AfterEach
    fun tearDown() { Dispatchers.resetMain() }

    private fun vm() = HomeMapViewModel(
        spotListService,
        spotMapService,
        locationService,
        mockk<SpotService>(relaxed = true),
        mockk<AuthService>(relaxed = true),
        mockk<BookmarkService>(relaxed = true),
        mockk<ExternalAppLauncher>(relaxed = true),
    )

    @Test
    fun `load emits Loaded with raw spots`() = runTest(testDispatcher) {
        val spot = Spot("s1", "n", SpotTheme.SUNSET, 0.0, 0.0)
        coEvery { spotListService.fetch(theme = null, page = 0) } returns
            SpotPage(items = listOf(spot), page = 0, hasNext = false)

        val viewModel = vm()
        viewModel.load(); advanceUntilIdle()
        val state = viewModel.curationSpots.value
        assertTrue(state is LoadState.Loaded && state.value.size == 1)
    }

    @Test
    fun `load emits Empty when no spots`() = runTest(testDispatcher) {
        coEvery { spotListService.fetch(theme = null, page = 0) } returns
            SpotPage(items = emptyList(), page = 0, hasNext = false)

        val viewModel = vm()
        viewModel.load(); advanceUntilIdle()
        assertEquals(LoadState.Empty, viewModel.curationSpots.value)
    }

    @Test
    fun `setZoom without prior viewport reloads via load`() = runTest(testDispatcher) {
        coEvery { spotListService.fetch(theme = null, page = 0) } returns
            SpotPage(items = emptyList(), page = 0, hasNext = false)

        val viewModel = vm()
        viewModel.setZoom(14); advanceUntilIdle()
        assertEquals(14, viewModel.zoom.value)
    }

    @Test
    fun `selectMood toggles theme on and off`() = runTest(testDispatcher) {
        coEvery { spotListService.fetch(theme = null, page = 0) } returns
            SpotPage(items = emptyList(), page = 0, hasNext = false)
        coEvery { spotListService.fetch(theme = SpotTheme.SUNSET, page = 0) } returns
            SpotPage(items = emptyList(), page = 0, hasNext = false)

        val viewModel = vm()
        viewModel.selectMood(MoodFilter.Sunset); advanceUntilIdle()
        assertEquals(MoodFilter.Sunset, viewModel.selectedMood.value)

        viewModel.selectMood(MoodFilter.Sunset); advanceUntilIdle()
        assertEquals(null, viewModel.selectedMood.value)
    }

    @Test
    fun `selectMapListMode switches between MAP and LIST`() {
        val viewModel = vm()
        assertEquals(MapListMode.MAP, viewModel.mapListMode.value)
        viewModel.selectMapListMode(MapListMode.LIST)
        assertEquals(MapListMode.LIST, viewModel.mapListMode.value)
        viewModel.selectMapListMode(MapListMode.MAP)
        assertEquals(MapListMode.MAP, viewModel.mapListMode.value)
    }
}
