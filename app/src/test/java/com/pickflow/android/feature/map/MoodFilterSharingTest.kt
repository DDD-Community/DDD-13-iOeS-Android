package com.pickflow.android.feature.map

import com.pickflow.android.core.services.impl.InMemoryMoodFilterStore
import com.pickflow.android.core.services.protocols.AuthService
import com.pickflow.android.core.services.protocols.BookmarkService
import com.pickflow.android.core.services.protocols.ExternalAppLauncher
import com.pickflow.android.core.services.protocols.LocationService
import com.pickflow.android.core.services.protocols.SpotListService
import com.pickflow.android.core.services.protocols.SpotMapService
import com.pickflow.android.core.services.protocols.SpotPage
import com.pickflow.android.core.services.protocols.SpotService
import com.pickflow.android.core.services.protocols.SpotTheme
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
 * 탐색 탭 무드 선택은 **지도와 리스트가 공유한다.**
 *
 * 두 화면은 별개 ViewModel 이지만 사용자에게는 같은 화면의 두 모드다.
 * 한쪽에서 고른 무드가 다른 쪽에도 즉시 반영되고, 반영된 쪽은 재조회까지 해야 한다.
 * 공유의 실체는 `@Singleton` [InMemoryMoodFilterStore] 하나를 두 ViewModel 이 함께 보는 것이다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MoodFilterSharingTest {

    private val testDispatcher = StandardTestDispatcher()

    /** 실제 앱에서 Hilt 가 @Singleton 으로 하나만 주입하는 것과 같은 조건. */
    private val store = InMemoryMoodFilterStore()

    private lateinit var listService: SpotListService
    private lateinit var mapService: SpotMapService

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        listService = mockk(relaxed = true)
        mapService = mockk(relaxed = true)
        coEvery { listService.fetch(any(), any(), any(), any()) } returns
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
        store,
    )

    private fun listVm() = SpotListViewModel(
        listService,
        mockk<BookmarkService>(relaxed = true),
        mockk<AuthService>(relaxed = true),
        mockk<LocationService>(relaxed = true),
        store,
    )

    @Test
    fun `selecting on the map is visible on the list`() = runTest(testDispatcher) {
        val map = mapVm()
        val list = listVm()
        advanceUntilIdle()

        map.selectMood(MoodFilter.Night)
        advanceUntilIdle()

        assertEquals(setOf(SpotTheme.NIGHT_VIEW), list.themes.value)
        assertEquals(setOf(MoodFilter.Night), map.selectedMoods.value)
    }

    @Test
    fun `selecting on the list is visible on the map`() = runTest(testDispatcher) {
        val map = mapVm()
        val list = listVm()
        advanceUntilIdle()

        list.toggleTheme(SpotTheme.SUNLIGHT)
        advanceUntilIdle()

        assertEquals(setOf(MoodFilter.Sunlight), map.selectedMoods.value)
    }

    @Test
    fun `multi selection accumulates across both screens`() = runTest(testDispatcher) {
        val map = mapVm()
        val list = listVm()
        advanceUntilIdle()

        map.selectMood(MoodFilter.Sunlight)
        list.toggleTheme(SpotTheme.NIGHT_VIEW)
        advanceUntilIdle()

        assertEquals(setOf(SpotTheme.SUNLIGHT, SpotTheme.NIGHT_VIEW), list.themes.value)
        assertEquals(setOf(MoodFilter.Sunlight, MoodFilter.Night), map.selectedMoods.value)

        // 재탭은 그 하나만 해제 — 어느 화면에서 눌러도 동일.
        list.toggleTheme(SpotTheme.SUNLIGHT)
        advanceUntilIdle()
        assertEquals(setOf(MoodFilter.Night), map.selectedMoods.value)
    }

    @Test
    fun `the other screen refetches when the shared selection changes`() = runTest(testDispatcher) {
        val map = mapVm()
        listVm()
        advanceUntilIdle()

        // 지도에서 토글 → 리스트 ViewModel 도 구독을 통해 스스로 재조회한다.
        map.selectMood(MoodFilter.Sunlight)
        advanceUntilIdle()

        coVerify(atLeast = 1) {
            listService.fetch(themes = setOf(SpotTheme.SUNLIGHT), page = 0, coordinates = any(), sort = any())
        }
    }

    @Test
    fun `clear returns both screens to the unfiltered state`() = runTest(testDispatcher) {
        val map = mapVm()
        val list = listVm()
        advanceUntilIdle()

        map.selectMood(MoodFilter.Sunlight)
        advanceUntilIdle()
        store.clear()
        advanceUntilIdle()

        assertEquals(emptySet<SpotTheme>(), list.themes.value)
        assertEquals(emptySet<MoodFilter>(), map.selectedMoods.value)
    }
}
