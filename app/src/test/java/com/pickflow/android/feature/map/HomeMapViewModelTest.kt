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
        coEvery { spotListService.fetch(themes = emptySet(), page = 0) } returns
            SpotPage(items = listOf(spot), page = 0, hasNext = false)

        val viewModel = vm()
        viewModel.load(); advanceUntilIdle()
        val state = viewModel.curationSpots.value
        assertTrue(state is LoadState.Loaded && state.value.size == 1)
    }

    @Test
    fun `load emits Empty when no spots`() = runTest(testDispatcher) {
        coEvery { spotListService.fetch(themes = emptySet(), page = 0) } returns
            SpotPage(items = emptyList(), page = 0, hasNext = false)

        val viewModel = vm()
        viewModel.load(); advanceUntilIdle()
        assertEquals(LoadState.Empty, viewModel.curationSpots.value)
    }

    @Test
    fun `setZoom without prior viewport reloads via load`() = runTest(testDispatcher) {
        coEvery { spotListService.fetch(themes = emptySet(), page = 0) } returns
            SpotPage(items = emptyList(), page = 0, hasNext = false)

        val viewModel = vm()
        viewModel.setZoom(14); advanceUntilIdle()
        assertEquals(14, viewModel.zoom.value)
    }

    @Test
    fun `selectMood accumulates multiple moods and unselects only the retapped one`() = runTest(testDispatcher) {
        coEvery { spotListService.fetch(themes = any(), page = 0) } returns
            SpotPage(items = emptyList(), page = 0, hasNext = false)

        val viewModel = vm()
        assertEquals(emptySet<MoodFilter>(), viewModel.selectedMoods.value)

        viewModel.selectMood(MoodFilter.Sunlight); advanceUntilIdle()
        assertEquals(setOf(MoodFilter.Sunlight), viewModel.selectedMoods.value)

        viewModel.selectMood(MoodFilter.Night); advanceUntilIdle()
        assertEquals(setOf(MoodFilter.Sunlight, MoodFilter.Night), viewModel.selectedMoods.value)

        // 재탭한 하나만 빠지고 나머지는 유지된다.
        viewModel.selectMood(MoodFilter.Sunlight); advanceUntilIdle()
        assertEquals(setOf(MoodFilter.Night), viewModel.selectedMoods.value)

        viewModel.selectMood(MoodFilter.Night); advanceUntilIdle()
        assertEquals(emptySet<MoodFilter>(), viewModel.selectedMoods.value)
    }

    @Test
    fun `selectMood maps moods to domain themes when fetching`() = runTest(testDispatcher) {
        coEvery { spotListService.fetch(themes = any(), page = 0) } returns
            SpotPage(items = emptyList(), page = 0, hasNext = false)

        val viewModel = vm()
        viewModel.selectMood(MoodFilter.Sunlight); advanceUntilIdle()
        viewModel.selectMood(MoodFilter.Reflection); advanceUntilIdle()

        coVerify {
            spotListService.fetch(themes = setOf(SpotTheme.SUNLIGHT, SpotTheme.YUNSEUL), page = 0)
        }
    }

    @Test
    fun `no mood selected fetches without theme filter`() = runTest(testDispatcher) {
        coEvery { spotListService.fetch(themes = emptySet(), page = 0) } returns
            SpotPage(items = emptyList(), page = 0, hasNext = false)

        val viewModel = vm()
        viewModel.load(); advanceUntilIdle()

        coVerify { spotListService.fetch(themes = emptySet(), page = 0) }
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
