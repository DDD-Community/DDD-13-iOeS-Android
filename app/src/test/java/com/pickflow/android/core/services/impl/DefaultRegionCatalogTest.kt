package com.pickflow.android.core.services.impl

import androidx.test.core.app.ApplicationProvider
import com.pickflow.android.core.network.ApiResponse
import com.pickflow.android.core.network.api.RegionApi
import com.pickflow.android.core.network.dto.region.RegionItemDto
import com.pickflow.android.core.network.dto.region.RegionListResponseDto
import com.pickflow.android.core.services.protocols.Region
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * `GET /v1/regions` + DataStore 캐시. 캐시가 실제로 남아 다음 실행에서 읽히는지가 요점이다.
 */
@RunWith(RobolectricTestRunner::class)
class DefaultRegionCatalogTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val api = mockk<RegionApi>()
    private fun catalog() = DefaultRegionCatalog(context, api)

    private fun respond(vararg pairs: Pair<Long, String>) {
        coEvery { api.getActiveRegions() } returns ApiResponse(
            success = true,
            data = RegionListResponseDto(pairs.map { RegionItemDto(it.first, it.second) }),
        )
    }

    /** 서버 응답을 도메인으로 옮기고, 노출 순서(내림차순)로 정렬한다. */
    @Test
    fun `refresh maps the server response`() = runTest {
        respond(1L to "서울", 2L to "대전")

        assertEquals(listOf(Region.Daejeon, Region.Seoul), catalog().refresh())
    }

    /** 서버가 오름차순으로 줘도 노출 순서는 regionId 내림차순(대전 → 서울)이다. */
    @Test
    fun `refresh normalizes the order by region id`() = runTest {
        respond(1L to "서울", 2L to "대전")

        assertEquals(listOf(Region.Daejeon, Region.Seoul), catalog().refresh())
    }

    /** 예전 순서로 남은 캐시도 읽을 때 내림차순으로 되돌린다. */
    @Test
    fun `cached normalizes a stale cache order`() = runTest {
        respond(1L to "서울", 2L to "대전")
        catalog().refresh()

        assertEquals(listOf(Region.Daejeon, Region.Seoul), catalog().cached())
    }

    /** refresh 가 캐시에 남아 새 인스턴스(= 앱 재시작)에서도 읽힌다. */
    @Test
    fun `refresh writes a cache the next instance can read`() = runTest {
        respond(2L to "대전")
        catalog().refresh()

        assertEquals(listOf(Region.Daejeon), catalog().cached())
    }

    /** 지도·리스트를 오가도 서버는 프로세스당 한 번만 친다(목록은 거의 안 바뀐다). */
    @Test
    fun `refresh hits the server only once per process`() = runTest {
        respond(1L to "서울")
        val catalog = catalog()

        assertEquals(listOf(Region.Seoul), catalog.refresh())
        assertEquals(null, catalog.refresh())
        coVerify(exactly = 1) { api.getActiveRegions() }
    }

    /** 호출이 실패하면 null — 스토어가 캐시/직전 값을 유지하도록. */
    @Test
    fun `refresh returns null when the call fails`() = runTest {
        coEvery { api.getActiveRegions() } throws java.io.IOException("offline")

        assertEquals(null, catalog().refresh())
    }

    /** 빈 목록은 캐시를 덮어쓰지 않는다 — 지역이 하나도 없으면 스팟 조회가 불가능하다. */
    @Test
    fun `refresh ignores an empty region list`() = runTest {
        respond(1L to "서울")
        catalog().refresh()
        respond()

        assertEquals(null, catalog().refresh())
        assertEquals(listOf(Region.Seoul), catalog().cached())
    }
}
