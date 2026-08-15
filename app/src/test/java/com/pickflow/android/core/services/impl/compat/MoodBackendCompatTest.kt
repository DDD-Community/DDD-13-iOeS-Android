package com.pickflow.android.core.services.impl.compat

import com.pickflow.android.core.services.protocols.Coordinates
import com.pickflow.android.core.services.protocols.Spot
import com.pickflow.android.core.services.protocols.SpotTheme
import com.pickflow.android.core.services.protocols.ViewportBox
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * PV-59 임시 호환 계층 테스트 — 백엔드 완료 시 이 파일도 함께 삭제한다.
 *
 * 서버 실측 사실(2026-08-04)을 코드로 고정해 둔다:
 * 서버는 SUNSET/YUNSEUL 단일만 처리하고, 반복 파라미터는 200을 주면서 첫 값만 적용한다.
 */
class MoodBackendCompatTest {

    private fun spot(id: String, theme: SpotTheme) =
        Spot(id = id, name = id, theme = theme, latitude = 0.0, longitude = 0.0)

    @Test
    fun `single server-known theme is passed through to the server`() {
        assertEquals(
            setOf(SpotTheme.SUNSET),
            MoodBackendCompat.serverQueryThemes(setOf(SpotTheme.SUNSET)),
        )
    }

    @Test
    fun `two server-known themes fall back to an unfiltered request`() {
        // 서버가 반복 파라미터의 첫 값만 적용하므로, 두 개일 땐 아예 안 보내고 클라에서 거른다.
        assertEquals(
            emptySet<SpotTheme>(),
            MoodBackendCompat.serverQueryThemes(setOf(SpotTheme.SUNSET, SpotTheme.YUNSEUL)),
        )
    }

    @Test
    fun `unknown themes are never sent to the server`() {
        // SUNLIGHT/NIGHT 를 보내면 서버가 400 C002 를 돌려준다.
        assertEquals(
            emptySet<SpotTheme>(),
            MoodBackendCompat.serverQueryThemes(setOf(SpotTheme.SUNLIGHT, SpotTheme.NIGHT_VIEW)),
        )
        assertEquals(
            setOf(SpotTheme.YUNSEUL),
            MoodBackendCompat.serverQueryThemes(setOf(SpotTheme.YUNSEUL, SpotTheme.NIGHT_VIEW)),
        )
    }

    @Test
    fun `network is skipped when only stub-only moods are selected`() {
        assertTrue(MoodBackendCompat.shouldSkipNetwork(setOf(SpotTheme.SUNLIGHT)))
        assertTrue(MoodBackendCompat.shouldSkipNetwork(setOf(SpotTheme.SUNLIGHT, SpotTheme.NIGHT_VIEW)))
        assertFalse(MoodBackendCompat.shouldSkipNetwork(setOf(SpotTheme.SUNSET, SpotTheme.NIGHT_VIEW)))
        // 전체 해제는 "전체 조회"이지 skip 이 아니다.
        assertFalse(MoodBackendCompat.shouldSkipNetwork(emptySet()))
    }

    @Test
    fun `client side filter keeps only the selected server-known themes`() {
        val items = listOf(
            spot("a", SpotTheme.SUNSET),
            spot("b", SpotTheme.YUNSEUL),
            spot("c", SpotTheme.SUNSET),
        )
        val filtered = MoodBackendCompat.filterServerItems(
            items = items,
            selected = setOf(SpotTheme.YUNSEUL, SpotTheme.NIGHT_VIEW),
            themeOf = { it.theme },
        )
        assertEquals(listOf("b"), filtered.map { it.id })
    }

    @Test
    fun `empty selection keeps every server item`() {
        val items = listOf(spot("a", SpotTheme.SUNSET), spot("b", SpotTheme.YUNSEUL))
        assertEquals(items, MoodBackendCompat.filterServerItems(items, emptySet()) { it.theme })
    }

    @Test
    fun `stub spots are produced only for moods the server does not know`() {
        assertTrue(MoodBackendCompat.stubSpots(setOf(SpotTheme.SUNSET)).isEmpty())

        val stubs = MoodBackendCompat.stubSpots(setOf(SpotTheme.SUNLIGHT, SpotTheme.SUNSET))
        assertTrue(stubs.isNotEmpty())
        assertTrue(stubs.all { it.theme == SpotTheme.SUNLIGHT })
        // 실데이터와 눈으로 구분되도록 접두사를 붙인다.
        assertTrue(stubs.all { it.name.startsWith("[STUB]") })
        // 그리드 key 중복 방지 — id 는 서로 달라야 한다.
        assertEquals(stubs.size, stubs.map { it.id }.toSet().size)
    }

    @Test
    fun `stub markers land inside the requested viewport`() {
        val box = ViewportBox(
            topLeft = Coordinates(37.60, 126.90),
            topRight = Coordinates(37.60, 127.10),
            bottomLeft = Coordinates(37.50, 126.90),
            bottomRight = Coordinates(37.50, 127.10),
        )
        val markers = MoodBackendCompat.stubMarkers(box, setOf(SpotTheme.SUNLIGHT, SpotTheme.NIGHT_VIEW))
        assertTrue(markers.isNotEmpty())
        markers.forEach {
            assertTrue(it.coordinates.latitude in 37.50..37.60, "lat=${it.coordinates.latitude}")
            assertTrue(it.coordinates.longitude in 126.90..127.10, "lng=${it.coordinates.longitude}")
        }
        assertEquals(markers.size, markers.map { it.spotId }.toSet().size)
    }

    @Test
    fun `no stub markers when only server-known moods are selected`() {
        val box = ViewportBox(
            topLeft = Coordinates(37.60, 126.90),
            topRight = Coordinates(37.60, 127.10),
            bottomLeft = Coordinates(37.50, 126.90),
            bottomRight = Coordinates(37.50, 127.10),
        )
        assertTrue(MoodBackendCompat.stubMarkers(box, setOf(SpotTheme.SUNSET)).isEmpty())
        assertTrue(MoodBackendCompat.stubMarkers(box, emptySet()).isEmpty())
    }

    @Test
    fun `flag is still off - flip it and delete this layer when the backend ships`() {
        assertFalse(
            MoodBackendCompat.BACKEND_SUPPORTS_MOOD_V2,
            "백엔드가 준비됐다면 이 호환 계층을 삭제할 차례다 — docs/PV-59/backend-compat-rollback.md",
        )
    }
}
