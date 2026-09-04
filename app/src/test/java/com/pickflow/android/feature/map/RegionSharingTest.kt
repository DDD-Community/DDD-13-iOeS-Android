package com.pickflow.android.feature.map

import com.pickflow.android.core.services.impl.InMemoryMoodFilterStore
import com.pickflow.android.core.services.impl.InMemoryRegionStore
import com.pickflow.android.core.services.protocols.AuthService
import com.pickflow.android.core.services.protocols.BookmarkService
import com.pickflow.android.core.services.protocols.ExternalAppLauncher
import com.pickflow.android.core.services.protocols.LocationService
import com.pickflow.android.core.services.protocols.Region
import com.pickflow.android.core.services.protocols.SpotListService
import com.pickflow.android.core.services.protocols.SpotMapService
import com.pickflow.android.core.services.protocols.SpotPage
import com.pickflow.android.core.services.protocols.SpotService
import com.pickflow.android.feature.spotlist.SpotListViewModel
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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * PV-65 — 지역 선택은 **지도와 리스트가 공유한다.**
 *
 * 무드와 같은 구조([MoodFilterSharingTest])지만 이유가 하나 더 있다. `regionId` 는
 * 스팟 조회 API 의 **필수** 파라미터라, 지역이 바뀌면 두 화면 모두 새 regionId 로 다시
 * 받아야 한다. 공유의 실체는 `@Singleton` [InMemoryRegionStore] 하나다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RegionSharingTest {

    private val testDispatcher = StandardTestDispatcher()

    /** 실제 앱에서 Hilt 가 @Singleton 으로 하나만 주입하는 것과 같은 조건. */
    private val moodStore = InMemoryMoodFilterStore()
    private val regionStore = InMemoryRegionStore()

    private lateinit var listService: SpotListService
    private lateinit var mapService: SpotMapService

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        listService = mockk(relaxed = true)
        mapService = mockk(relaxed = true)
        coEvery { listService.fetch(any(), any(), any(), any(), any()) } returns
            SpotPage(items = emptyList(), page = 0, hasNext = false)
    }

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    private fun mapVm() = HomeMapViewModel(
        listService,
        mapService,
        mockk<LocationService>(relaxed = true),
        mockk<SpotService>(relaxed = true),
        mockk<AuthService>(relaxed = true),
        mockk<BookmarkService>(relaxed = true),
        mockk<ExternalAppLauncher>(relaxed = true),
        moodStore,
        regionStore,
    )

    private fun listVm() = SpotListViewModel(
        listService,
        mockk<BookmarkService>(relaxed = true),
        mockk<AuthService>(relaxed = true),
        mockk<LocationService>(relaxed = true),
        moodStore,
        regionStore,
    )

    @Test
    fun `both screens start on Seoul`() = runTest(testDispatcher) {
        assertEquals(Region.Seoul, mapVm().region.value)
        assertEquals(Region.Seoul, listVm().region.value)
    }

    @Test
    fun `applying a region on the map moves the list to the same region`() = runTest(testDispatcher) {
        val map = mapVm()
        val list = listVm()
        advanceUntilIdle()

        map.applyRegion(Region.Daejeon)
        advanceUntilIdle()

        assertEquals(Region.Daejeon, list.region.value)
    }

    @Test
    fun `the list refetches with the new regionId when the map applies a region`() = runTest(testDispatcher) {
        val map = mapVm()
        listVm()
        advanceUntilIdle()

        map.applyRegion(Region.Daejeon)
        advanceUntilIdle()

        coVerify(atLeast = 1) {
            listService.fetch(
                themes = any(),
                page = 0,
                region = Region.Daejeon,
                coordinates = any(),
                sort = any(),
            )
        }
    }

    /** 지도 자신도 새 regionId 로 목록을 다시 받는다 — 카메라 이동만으로는 부족하다. */
    @Test
    fun `the map refetches with the new regionId`() = runTest(testDispatcher) {
        val map = mapVm()
        advanceUntilIdle()

        map.applyRegion(Region.Daejeon)
        advanceUntilIdle()

        coVerify(atLeast = 1) { listService.fetch(themes = any(), page = 0, region = Region.Daejeon) }
    }
}
