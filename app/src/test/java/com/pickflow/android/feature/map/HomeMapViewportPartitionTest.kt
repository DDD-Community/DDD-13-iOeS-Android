package com.pickflow.android.feature.map

import com.pickflow.android.common.ui.LoadState
import com.pickflow.android.core.services.impl.InMemoryMoodFilterStore
import com.pickflow.android.core.services.protocols.AuthService
import com.pickflow.android.core.services.protocols.BookmarkService
import com.pickflow.android.core.services.protocols.Coordinates
import com.pickflow.android.core.services.protocols.ExternalAppLauncher
import com.pickflow.android.core.services.protocols.LocationService
import com.pickflow.android.core.services.protocols.Spot
import com.pickflow.android.core.services.protocols.SpotListService
import com.pickflow.android.core.services.protocols.SpotMapMarker
import com.pickflow.android.core.services.protocols.SpotMapService
import com.pickflow.android.core.services.protocols.SpotService
import com.pickflow.android.core.services.protocols.SpotTheme
import com.pickflow.android.core.services.protocols.ViewportBox
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
class HomeMapViewportPartitionTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var listService: SpotListService
    private lateinit var mapService: SpotMapService
    private lateinit var locationService: LocationService

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        listService = mockk(relaxed = true)
        mapService = mockk(relaxed = true)
        locationService = mockk(relaxed = true)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun vm() = HomeMapViewModel(
        listService,
        mapService,
        locationService,
        mockk<SpotService>(relaxed = true),
        mockk<AuthService>(relaxed = true),
        mockk<BookmarkService>(relaxed = true),
        mockk<ExternalAppLauncher>(relaxed = true),
        InMemoryMoodFilterStore(),
    )

    private fun box() = ViewportBox(
        topLeft = Coordinates(37.6, 126.9),
        topRight = Coordinates(37.6, 127.1),
        bottomLeft = Coordinates(37.5, 126.9),
        bottomRight = Coordinates(37.5, 127.1),
    )

    private fun marker(id: Long, isMine: Boolean) = SpotMapMarker(
        spotId = id,
        imageUrl = null,
        coordinates = Coordinates(37.55, 127.0),
        isMySpot = isMine,
    )

    @Test
    fun `viewport response partitions curation into curationSpots and mySpots`() = runTest(testDispatcher) {
        coEvery { mapService.fetchInViewport(any(), any()) } returns listOf(
            marker(1, isMine = false),
            marker(2, isMine = true),
            marker(3, isMine = false),
            marker(4, isMine = true),
        )

        val viewModel = vm()
        viewModel.onViewportChanged(box(), 12)
        advanceUntilIdle()

        // curationSpots 는 isMySpot=false 인 1,3 만.
        val loaded = viewModel.curationSpots.value as LoadState.Loaded<List<Spot>>
        assertEquals(listOf("1", "3"), loaded.value.map { it.id })

        // mySpots 는 isMySpot=true 인 2,4.
        assertEquals(listOf(2L, 4L), viewModel.mySpots.value.map { it.spotId })
    }

    @Test
    fun `selectSpot updates selectedSpotId and selectedCluster`() = runTest(testDispatcher) {
        coEvery { mapService.fetchInViewport(any(), any()) } returns listOf(marker(7, false))
        val viewModel = vm()
        viewModel.onViewportChanged(box(), 12)
        advanceUntilIdle()

        viewModel.selectSpot(7L)
        assertEquals(7L, viewModel.selectedSpotId.value)
        assertEquals(listOf("7"), viewModel.selectedCluster.value?.spotIds)
    }

    @Test
    fun `dismissCluster clears both selectedCluster and selectedSpotId`() = runTest(testDispatcher) {
        val viewModel = vm()
        viewModel.selectCluster(Cluster(0.0, 0.0, 3, listOf("1", "2", "3")))
        assertEquals(1L, viewModel.selectedSpotId.value)
        viewModel.dismissCluster()
        assertEquals(null, viewModel.selectedSpotId.value)
        assertEquals(null, viewModel.selectedCluster.value)
    }

    @Test
    fun `viewport failure clears mySpots and emits Failed`() = runTest(testDispatcher) {
        coEvery { mapService.fetchInViewport(any(), any()) } throws RuntimeException("net")
        val viewModel = vm()
        viewModel.onViewportChanged(box(), 12)
        advanceUntilIdle()
        assertEquals(emptyList<MySpotMarker>(), viewModel.mySpots.value)
        assertTrue(viewModel.curationSpots.value is LoadState.Failed)
    }

    @Test
    fun `selectMood toggles theme and reissues viewport`() = runTest(testDispatcher) {
        coEvery { mapService.fetchInViewport(any(), any()) } returns emptyList()
        val viewModel = vm()
        viewModel.onViewportChanged(box(), 12)
        advanceUntilIdle()
        viewModel.selectMood(MoodFilter.Sunset)
        advanceUntilIdle()
        assertEquals(setOf(MoodFilter.Sunset), viewModel.selectedMoods.value)
        coVerify { mapService.fetchInViewport(any(), setOf(SpotTheme.SUNSET)) }
    }
}
