package com.pickflow.android.core.services.impl

import com.pickflow.android.core.services.protocols.Region
import com.pickflow.android.core.services.protocols.RegionCatalog
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * 지역 목록을 서버(캐시 포함)에서 받아 바텀시트에 반영하는 부분.
 *
 * 목록은 [RegionCatalog] 가 책임지고, 스토어는 "무엇을 띄우고 무엇이 선택돼 있는가"만 본다.
 */
class DefaultRegionStoreTest {

    private class FakeCatalog(
        private val cached: List<Region> = emptyList(),
        private val fetched: List<Region>? = null,
    ) : RegionCatalog {
        override suspend fun cached() = cached
        override suspend fun refresh() = fetched
    }

    private val busan = Region(3, "부산")

    /** 아직 아무것도 못 받았으면 부트스트랩 목록이 뜬다(시트가 비면 안 된다). */
    @Test
    fun `available starts from the fallback list`() {
        assertEquals(Region.FALLBACK, DefaultRegionStore(FakeCatalog()).available.value)
    }

    /** 서버 목록이 그대로 반영된다 — 앱이 모르던 지역도 포함해서. */
    @Test
    fun `refreshAvailable takes the server list as-is`() = runTest {
        val store = DefaultRegionStore(FakeCatalog(fetched = listOf(Region.Seoul, busan)))
        store.refreshAvailable()

        assertEquals(listOf(Region.Seoul, busan), store.available.value)
    }

    /** 좌표를 모르는 지역은 목록에 남되 center 가 null 이라 카메라를 옮기지 않는다. */
    @Test
    fun `a region the app has no coordinates for has a null center`() {
        assertEquals(null, busan.center)
        assertEquals(com.pickflow.android.core.services.protocols.Coordinates(37.538, 127.038), Region.Seoul.center)
    }

    /** 네트워크가 죽어도 캐시가 있으면 그걸 띄운다. */
    @Test
    fun `refreshAvailable falls back to the cache when the server call fails`() = runTest {
        val store = DefaultRegionStore(FakeCatalog(cached = listOf(busan), fetched = null))
        store.refreshAvailable()

        assertEquals(listOf(busan), store.available.value)
    }

    /** 캐시도 서버도 없으면 직전 목록을 유지한다. */
    @Test
    fun `refreshAvailable keeps the previous list when nothing is available`() = runTest {
        val store = DefaultRegionStore(FakeCatalog())
        store.refreshAvailable()

        assertEquals(Region.FALLBACK, store.available.value)
    }

    /** 빈 목록은 직전 값을 밀어내지 않는다(mock 카탈로그처럼 빈 리스트를 주는 구현도 있다). */
    @Test
    fun `refreshAvailable ignores an empty list`() = runTest {
        val store = DefaultRegionStore(FakeCatalog(cached = emptyList(), fetched = emptyList()))
        store.refreshAvailable()

        assertEquals(Region.FALLBACK, store.available.value)
        assertEquals(Region.Daejeon, store.selected.value)
    }

    /** 적용 중이던 지역이 목록에서 빠지면 죽은 regionId 로 조회하지 않도록 옮긴다. */
    @Test
    fun `refreshAvailable moves the selection off a region that disappeared`() = runTest {
        val store = DefaultRegionStore(FakeCatalog(fetched = listOf(Region.Seoul)))
        store.refreshAvailable()

        assertEquals(Region.Seoul, store.selected.value)
    }

    /** 같은 지역이 이름만 바뀌어 와도 선택은 유지된다(비교 기준은 regionId). */
    @Test
    fun `refreshAvailable keeps the selection when only the name changed`() = runTest {
        val store = DefaultRegionStore(FakeCatalog(fetched = listOf(Region(2, "대전광역시"))))
        store.refreshAvailable()

        assertEquals(2L, store.selected.value.id)
    }
}
