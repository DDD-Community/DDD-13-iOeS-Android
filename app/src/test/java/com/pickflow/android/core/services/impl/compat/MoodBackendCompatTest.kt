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
 * PV-85 로 빌드타입별 서버가 갈렸다(debug=개발, release=운영). 두 환경의 능력이 다르므로
 * 상수(`SERVER_KNOWN_THEMES`)에 의존하지 않고 **양쪽 형상을 명시적으로 주입해** 검증한다.
 *
 * 2026-08-18 실측 기준:
 * - 개발: 4종 모두 200 (`SUNLIGHT`→`SL`, `NIGHT_VIEW`→`NV`)
 * - 운영: 신규 2종은 400 C002
 * - **양쪽 다** 반복 파라미터는 200 이지만 첫 값만 적용
 */
class MoodBackendCompatTest {

    /** 운영 서버 형상 — 신규 2종을 모른다. */
    private val prod = setOf(SpotTheme.SUNSET, SpotTheme.YUNSEUL)

    /** 개발 서버 형상 — 4종 모두 안다. */
    private val dev = SpotTheme.entries.toSet()

    private fun spot(id: String, theme: SpotTheme) =
        Spot(id = id, name = id, theme = theme, latitude = 0.0, longitude = 0.0)

    // MARK: - 서버로 나가는 theme 결정

    @Test
    fun `single server-known theme is passed through`() {
        assertEquals(setOf(SpotTheme.SUNSET), MoodBackendCompat.serverQueryThemes(setOf(SpotTheme.SUNSET), prod))
        // 개발 서버는 신규 2종도 그대로 위임한다.
        assertEquals(setOf(SpotTheme.NIGHT_VIEW), MoodBackendCompat.serverQueryThemes(setOf(SpotTheme.NIGHT_VIEW), dev))
    }

    @Test
    fun `two themes fall back to an unfiltered request on both environments`() {
        // 반복 파라미터의 첫 값만 적용되므로, 두 개일 땐 아예 안 보내고 클라에서 거른다.
        assertEquals(emptySet<SpotTheme>(), MoodBackendCompat.serverQueryThemes(setOf(SpotTheme.SUNSET, SpotTheme.YUNSEUL), prod))
        assertEquals(emptySet<SpotTheme>(), MoodBackendCompat.serverQueryThemes(setOf(SpotTheme.SUNLIGHT, SpotTheme.NIGHT_VIEW), dev))
    }

    @Test
    fun `themes the server does not know are never sent`() {
        // 운영에 SUNLIGHT/NIGHT_VIEW 를 보내면 400 C002 가 온다.
        assertEquals(emptySet<SpotTheme>(), MoodBackendCompat.serverQueryThemes(setOf(SpotTheme.SUNLIGHT, SpotTheme.NIGHT_VIEW), prod))
        assertEquals(setOf(SpotTheme.YUNSEUL), MoodBackendCompat.serverQueryThemes(setOf(SpotTheme.YUNSEUL, SpotTheme.NIGHT_VIEW), prod))
    }

    // MARK: - 네트워크 skip

    @Test
    fun `network is skipped only when nothing selected is known to the server`() {
        assertTrue(MoodBackendCompat.shouldSkipNetwork(setOf(SpotTheme.SUNLIGHT), prod))
        assertTrue(MoodBackendCompat.shouldSkipNetwork(setOf(SpotTheme.SUNLIGHT, SpotTheme.NIGHT_VIEW), prod))
        assertFalse(MoodBackendCompat.shouldSkipNetwork(setOf(SpotTheme.SUNSET, SpotTheme.NIGHT_VIEW), prod))
        // 개발 서버는 4종을 다 아니까 skip 이 일어나지 않는다.
        assertFalse(MoodBackendCompat.shouldSkipNetwork(setOf(SpotTheme.SUNLIGHT, SpotTheme.NIGHT_VIEW), dev))
        // 전체 해제는 "전체 조회"이지 skip 이 아니다.
        assertFalse(MoodBackendCompat.shouldSkipNetwork(emptySet(), prod))
    }

    // MARK: - 클라이언트 필터

    @Test
    fun `client side filter keeps only the selected server-known themes`() {
        val items = listOf(
            spot("a", SpotTheme.SUNSET),
            spot("b", SpotTheme.YUNSEUL),
            spot("c", SpotTheme.SUNSET),
        )
        assertEquals(
            listOf("b"),
            MoodBackendCompat.filterServerItems(items, setOf(SpotTheme.YUNSEUL, SpotTheme.NIGHT_VIEW), prod) { it.theme }
                .map { it.id },
        )
    }

    @Test
    fun `client side filter passes through new themes on dev`() {
        val items = listOf(spot("a", SpotTheme.SUNLIGHT), spot("b", SpotTheme.SUNSET))
        assertEquals(
            listOf("a"),
            MoodBackendCompat.filterServerItems(items, setOf(SpotTheme.SUNLIGHT), dev) { it.theme }.map { it.id },
        )
    }

    @Test
    fun `empty selection keeps every server item`() {
        val items = listOf(spot("a", SpotTheme.SUNSET), spot("b", SpotTheme.YUNSEUL))
        assertEquals(items, MoodBackendCompat.filterServerItems(items, emptySet(), prod) { it.theme })
    }

    // MARK: - Stub

    @Test
    fun `stub spots are produced only for moods the server does not know`() {
        val stubOnlyProd = SpotTheme.entries.toSet() - prod

        assertTrue(MoodBackendCompat.stubSpots(setOf(SpotTheme.SUNSET), stubOnlyProd).isEmpty())

        val stubs = MoodBackendCompat.stubSpots(setOf(SpotTheme.SUNLIGHT, SpotTheme.SUNSET), stubOnlyProd)
        assertTrue(stubs.isNotEmpty())
        assertTrue(stubs.all { it.theme == SpotTheme.SUNLIGHT })
        // 실데이터와 눈으로 구분되도록 접두사를 붙인다.
        assertTrue(stubs.all { it.name.startsWith("[STUB]") })
        // 그리드 key 중복 방지 — id 는 서로 달라야 한다.
        assertEquals(stubs.size, stubs.map { it.id }.toSet().size)
    }

    @Test
    fun `no stub spots on dev - the server has real data for all four moods`() {
        val stubOnlyDev = SpotTheme.entries.toSet() - dev
        assertTrue(stubOnlyDev.isEmpty())
        assertTrue(MoodBackendCompat.stubSpots(SpotTheme.entries.toSet(), stubOnlyDev).isEmpty())
    }

    @Test
    fun `stub markers land inside the requested viewport`() {
        val stubOnlyProd = SpotTheme.entries.toSet() - prod
        val markers = MoodBackendCompat.stubMarkers(box(), setOf(SpotTheme.SUNLIGHT, SpotTheme.NIGHT_VIEW), stubOnlyProd)
        assertTrue(markers.isNotEmpty())
        markers.forEach {
            assertTrue(it.coordinates.latitude in 37.50..37.60, "lat=${it.coordinates.latitude}")
            assertTrue(it.coordinates.longitude in 126.90..127.10, "lng=${it.coordinates.longitude}")
        }
        assertEquals(markers.size, markers.map { it.spotId }.toSet().size)
    }

    @Test
    fun `no stub markers when only server-known moods are selected`() {
        val stubOnlyProd = SpotTheme.entries.toSet() - prod
        assertTrue(MoodBackendCompat.stubMarkers(box(), setOf(SpotTheme.SUNSET), stubOnlyProd).isEmpty())
        assertTrue(MoodBackendCompat.stubMarkers(box(), emptySet(), stubOnlyProd).isEmpty())
    }

    // MARK: - 현재 빌드 형상

    @Test
    fun `debug build points at the dev server which knows all four moods`() {
        // PV-85: debug = dev-api.pickflow-api.us. 2026-08-18 기준 신규 2종이 배포돼 있다.
        // 단위 테스트는 debug variant 로 돈다.
        assertEquals(SpotTheme.entries.toSet(), MoodBackendCompat.SERVER_KNOWN_THEMES)
        assertTrue(
            MoodBackendCompat.STUB_ONLY_THEMES.isEmpty(),
            "개발 서버에 실데이터가 있으므로 debug 빌드에는 stub 이 뜨면 안 된다",
        )
    }

    @Test
    fun `flag is still off - flip it and delete this layer when the backend ships multi filter`() {
        assertFalse(
            MoodBackendCompat.BACKEND_SUPPORTS_MOOD_V2,
            "다중 필터까지 지원되면 이 호환 계층을 삭제할 차례다 — docs/PV-59/backend-compat-rollback.md",
        )
    }

    private fun box() = ViewportBox(
        topLeft = Coordinates(37.60, 126.90),
        topRight = Coordinates(37.60, 127.10),
        bottomLeft = Coordinates(37.50, 126.90),
        bottomRight = Coordinates(37.50, 127.10),
    )
}
