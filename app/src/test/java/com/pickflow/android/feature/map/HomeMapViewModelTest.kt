package com.pickflow.android.feature.map

import com.pickflow.android.common.ui.LoadState
import com.pickflow.android.core.services.protocols.Cluster
import com.pickflow.android.core.services.protocols.ClusteringService
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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeMapViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var spotListService: SpotListService
    private lateinit var clusteringService: ClusteringService

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        spotListService = mockk()
        clusteringService = mockk()
    }

    @AfterEach
    fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `load emits Loaded clusters`() = runTest(testDispatcher) {
        val spot = Spot("s1", "n", SpotTheme.CAFE, 0.0, 0.0)
        coEvery { spotListService.fetch(null, null, 100) } returns
            SpotPage(listOf(spot), null)
        coEvery { clusteringService.cluster(any(), any()) } returns
            listOf(Cluster(0.0, 0.0, 1, listOf("s1")))

        val vm = HomeMapViewModel(spotListService, clusteringService)
        vm.load(); advanceUntilIdle()
        val state = vm.clusters.value
        assertTrue(state is LoadState.Loaded && state.value.size == 1)
    }

    @Test
    fun `load emits Empty when no clusters`() = runTest(testDispatcher) {
        coEvery { spotListService.fetch(null, null, 100) } returns SpotPage(emptyList(), null)
        coEvery { clusteringService.cluster(any(), any()) } returns emptyList()

        val vm = HomeMapViewModel(spotListService, clusteringService)
        vm.load(); advanceUntilIdle()
        assertEquals(LoadState.Empty, vm.clusters.value)
    }

    @Test
    fun `setZoom updates and reloads`() = runTest(testDispatcher) {
        coEvery { spotListService.fetch(null, null, 100) } returns SpotPage(emptyList(), null)
        coEvery { clusteringService.cluster(any(), any()) } returns emptyList()

        val vm = HomeMapViewModel(spotListService, clusteringService)
        vm.setZoom(14); advanceUntilIdle()
        assertEquals(14, vm.zoom.value)
    }

    @Test
    fun `selectMood toggles and filters spots by mood`() = runTest(testDispatcher) {
        val s0 = Spot("s0", "n0", SpotTheme.CAFE, 0.0, 0.0)
        val s1 = Spot("s1", "n1", SpotTheme.BAR, 0.0, 0.0)
        coEvery { spotListService.fetch(null, null, 100) } returns SpotPage(listOf(s0, s1), null)
        coEvery { clusteringService.cluster(any(), any()) } returns
            listOf(Cluster(0.0, 0.0, 1, listOf("s0")))

        val vm = HomeMapViewModel(spotListService, clusteringService)
        vm.selectMood(MoodFilter.Sunset); advanceUntilIdle()
        assertEquals(MoodFilter.Sunset, vm.selectedMood.value)
        // Sunset = 짝수 인덱스만 → s0 1개로 클러스터링.
        io.mockk.coVerify { clusteringService.cluster(match { it.size == 1 }, any()) }

        vm.selectMood(MoodFilter.Sunset); advanceUntilIdle()
        assertEquals(null, vm.selectedMood.value) // 같은 무드 재선택 → 해제
    }

    @Test
    fun `selectCluster and dismissCluster update selectedCluster`() {
        val vm = HomeMapViewModel(spotListService, clusteringService)
        val cluster = Cluster(1.0, 2.0, 3, listOf("a", "b", "c"))
        vm.selectCluster(cluster)
        assertEquals(cluster, vm.selectedCluster.value)
        vm.dismissCluster()
        assertEquals(null, vm.selectedCluster.value)
    }

    @Test
    fun `selectMapListMode switches between MAP and LIST`() = runTest(testDispatcher) {
        val vm = HomeMapViewModel(spotListService, clusteringService)
        assertEquals(MapListMode.MAP, vm.mapListMode.value)
        vm.selectMapListMode(MapListMode.LIST)
        assertEquals(MapListMode.LIST, vm.mapListMode.value)
        vm.selectMapListMode(MapListMode.MAP)
        assertEquals(MapListMode.MAP, vm.mapListMode.value)
    }
}
