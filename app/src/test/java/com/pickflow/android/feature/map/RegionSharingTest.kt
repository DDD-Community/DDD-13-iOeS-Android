package com.pickflow.android.feature.map

import com.pickflow.android.core.services.impl.InMemoryMoodFilterStore
import com.pickflow.android.core.services.impl.DefaultRegionStore
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
 * 받아야 한다. 공유의 실체는 `@Singleton` [DefaultRegionStore] 하나다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RegionSharingTest {

    private val testDispatcher = StandardTestDispatcher()

    /** 실제 앱에서 Hilt 가 @Singleton 으로 하나만 주입하는 것과 같은 조건. */
    private val moodStore = InMemoryMoodFilterStore()
    private val regionStore = DefaultRegionStore(mockk(relaxed = true))

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
    fun `both screens start on Daejeon`() = runTest(testDispatcher) {
        assertEquals(Region.Daejeon, mapVm().region.value)
        assertEquals(Region.Daejeon, listVm().region.value)
    }

    @Test
    fun `applying a region on the map moves the list to the same region`() = runTest(testDispatcher) {
        val map = mapVm()
        val list = listVm()
        advanceUntilIdle()

        map.applyRegion(Region.Seoul)
        advanceUntilIdle()

        assertEquals(Region.Seoul, list.region.value)
    }

    @Test
    fun `the list refetches with the new regionId when the map applies a region`() = runTest(testDispatcher) {
        val map = mapVm()
        listVm()
        advanceUntilIdle()

        map.applyRegion(Region.Seoul)
        advanceUntilIdle()

        coVerify(atLeast = 1) {
            listService.fetch(
                themes = any(),
                page = 0,
                region = Region.Seoul,
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

        map.applyRegion(Region.Seoul)
        advanceUntilIdle()

        coVerify(atLeast = 1) { listService.fetch(themes = any(), page = 0, region = Region.Seoul) }
    }

    /**
     * 리스트 헤더에서 지역을 바꿔도 지도가 따라온다 — 지도의 반응은 store 구독에 걸려 있고
     * 바텀시트 콜백에 걸려 있지 않다는 뜻이다(어느 헤더에서 눌러도 같은 경로).
     */
    @Test
    fun `applying a region on the list moves the map camera and refetches`() = runTest(testDispatcher) {
        val map = mapVm()
        val list = listVm()
        advanceUntilIdle()

        list.applyRegion(Region.Seoul)
        advanceUntilIdle()

        assertEquals(Region.Seoul, map.region.value)
        assertEquals(Region.Seoul.center, map.regionTarget.value)
        coVerify(atLeast = 1) { listService.fetch(themes = any(), page = 0, region = Region.Seoul) }
    }

    /** 같은 지역 재적용은 카메라 이동도 재조회도 만들지 않는다. */
    @Test
    fun `applying the already applied region does nothing`() = runTest(testDispatcher) {
        val map = mapVm()
        advanceUntilIdle()

        map.applyRegion(Region.Daejeon)
        advanceUntilIdle()

        assertEquals(null, map.regionTarget.value)
    }
}
