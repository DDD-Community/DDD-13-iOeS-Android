package com.pickflow.android.core.services.impl.compat

import com.pickflow.android.BuildConfig
import com.pickflow.android.core.services.protocols.Coordinates
import com.pickflow.android.core.services.protocols.Spot
import com.pickflow.android.core.services.protocols.SpotMapMarker
import com.pickflow.android.core.services.protocols.SpotTheme
import com.pickflow.android.core.services.protocols.ViewportBox

/**
 * PV-59 임시 호환 계층 — **백엔드 구현 완료 시 통째로 삭제한다.**
 *
 * ## 왜 필요한가
 *
 * PV-85 로 빌드타입별 서버가 갈렸고(debug=개발, release=운영), 두 환경의 능력이 다르다.
 * 2026-08-18 실측:
 *
 * | 요청 | 개발(debug) | 운영(release) |
 * |---|---|---|
 * | `?theme=SUNSET` / `?theme=YUNSEUL` | 200 | 200 |
 * | `?theme=SUNLIGHT` | **200** `{SL:6}` | **400 C002** |
 * | `?theme=NIGHT_VIEW` | **200** `{NV:2}` | **400 C002** |
 * | `?theme=A&theme=B` (반복) | **200 이지만 첫 값만 적용** | 동일 |
 * | `?theme=A,B` (CSV) | 400 | 400 |
 *
 * 즉 **신규 2종은 개발 서버에만 배포돼 있고, 다중 필터는 양쪽 다 미지원**이다.
 * 다중 쪽이 특히 위험하다 — 400이 아니라 200을 주므로 오작동을 감지할 수 없다.
 *
 * ## 무엇을 하는가
 *
 * - 서버가 아는 무드(노을/윤슬)는 **실서버 응답을 그대로** 쓴다.
 * - 2개 이상 선택돼 서버가 처리 못 하면 `theme` 없이 전체를 받아 **클라이언트에서 필터**한다.
 * - 서버가 모르는 무드(햇살/야경)는 [stubSpots] / [stubMarkers]로 채워 신규 UI를 볼 수 있게 한다.
 *   stub 스팟은 이름에 `[STUB]` 접두사가 붙어 실데이터와 구분된다.
 *
 * ## 되돌리는 법
 *
 * [BACKEND_SUPPORTS_MOOD_V2]를 `true`로 바꾸면 데코레이터가 즉시 통과(pass-through)로 바뀐다.
 * 완전 제거 절차는 `docs/PV-59/backend-compat-rollback.md` 참고.
 */
object MoodBackendCompat {

    /**
     * 백엔드가 신규 무드 2종 + 다중 `theme` 파라미터를 지원하면 `true`로 바꾼다.
     * `true`가 되는 순간 이 파일의 모든 우회 로직이 비활성화되고 원래 설계대로 동작한다.
     */
    const val BACKEND_SUPPORTS_MOOD_V2 = false

    /**
     * 서버가 `theme` 쿼리로 받아주는 값. 나머지를 보내면 400 이 온다.
     *
     * **빌드타입에 따라 다르다**(PV-85 로 debug=개발 서버, release=운영 서버로 분리).
     * 개발 서버에는 신규 2종이 배포돼 있어 실데이터가 나오므로 stub 이 필요 없다.
     */
    val SERVER_KNOWN_THEMES: Set<SpotTheme> =
        if (BuildConfig.DEBUG) SpotTheme.entries.toSet()
        else setOf(SpotTheme.SUNSET, SpotTheme.YUNSEUL)

    /**
     * 서버가 아직 모르는 값 — stub 으로 대체한다.
     * 개발 서버(debug)에서는 비어 있다 = **debug 빌드에는 stub 이 뜨지 않는다.**
     */
    val STUB_ONLY_THEMES: Set<SpotTheme> = SpotTheme.entries.toSet() - SERVER_KNOWN_THEMES

    /**
     * 서버에 실제로 보낼 `theme` 집합.
     *
     * 서버는 값을 1개만 처리하므로, 2개 이상이면 빈 Set(=전체 조회)을 돌려주고
     * 걸러내는 일은 [filterServerItems]가 맡는다.
     */
    fun serverQueryThemes(
        selected: Set<SpotTheme>,
        serverKnown: Set<SpotTheme> = SERVER_KNOWN_THEMES,
    ): Set<SpotTheme> {
        val known = selected intersect serverKnown
        return if (known.size == 1) known else emptySet()
    }

    /** 서버가 아는 무드가 하나도 선택되지 않았다면 네트워크를 탈 이유가 없다. */
    fun shouldSkipNetwork(
        selected: Set<SpotTheme>,
        serverKnown: Set<SpotTheme> = SERVER_KNOWN_THEMES,
    ): Boolean = selected.isNotEmpty() && (selected intersect serverKnown).isEmpty()

    /**
     * 서버 응답을 선택된 무드로 다시 거른다.
     *
     * 서버가 1개만 필터해 준 경우엔 이미 걸러져 있어 no-op 이고,
     * 2개 이상이라 전체를 받아온 경우엔 여기서 실제 필터링이 일어난다.
     */
    fun <T> filterServerItems(
        items: List<T>,
        selected: Set<SpotTheme>,
        serverKnown: Set<SpotTheme> = SERVER_KNOWN_THEMES,
        themeOf: (T) -> SpotTheme,
    ): List<T> {
        if (selected.isEmpty()) return items
        val known = selected intersect serverKnown
        return items.filter { themeOf(it) in known }
    }

    // MARK: - Stub 데이터

    /** 선택된 무드 중 서버가 모르는 것들에 대한 가짜 스팟. 이름에 `[STUB]` 접두사. */
    fun stubSpots(
        selected: Set<SpotTheme>,
        stubOnly: Set<SpotTheme> = STUB_ONLY_THEMES,
    ): List<Spot> =
        (selected intersect stubOnly).flatMap { theme ->
            stubSeeds(theme).map { seed ->
                Spot(
                    id = seed.id,
                    name = "[STUB] ${seed.name}",
                    theme = theme,
                    latitude = seed.lat,
                    longitude = seed.lng,
                    imageUrl = null,
                    address = "서울특별시",
                    distanceKm = seed.distanceKm,
                )
            }
        }

    /** 지도 viewport 용 가짜 마커 — 현재 보이는 영역 중앙 근처에 흩뿌린다. */
    fun stubMarkers(
        box: ViewportBox,
        selected: Set<SpotTheme>,
        stubOnly: Set<SpotTheme> = STUB_ONLY_THEMES,
    ): List<SpotMapMarker> {
        val themes = selected intersect stubOnly
        if (themes.isEmpty()) return emptyList()
        val centerLat = (box.topLeft.latitude + box.bottomLeft.latitude) / 2
        val centerLng = (box.topLeft.longitude + box.topRight.longitude) / 2
        val latSpan = (box.topLeft.latitude - box.bottomLeft.latitude).coerceAtLeast(0.001)
        val lngSpan = (box.topRight.longitude - box.topLeft.longitude).coerceAtLeast(0.001)

        return themes.flatMapIndexed { themeIndex, theme ->
            stubSeeds(theme).mapIndexed { i, seed ->
                SpotMapMarker(
                    spotId = seed.id.removePrefix(STUB_ID_PREFIX).hashCode().toLong().let { -kotlin.math.abs(it) },
                    imageUrl = null,
                    coordinates = Coordinates(
                        latitude = centerLat + latSpan * (0.12 * (i - 1)),
                        longitude = centerLng + lngSpan * (0.12 * (themeIndex * 2 + i - 1)),
                    ),
                    isMySpot = false,
                )
            }
        }
    }

    private const val STUB_ID_PREFIX = "stub-"

    private data class StubSeed(
        val id: String,
        val name: String,
        val lat: Double,
        val lng: Double,
        val distanceKm: Double,
    )

    private fun stubSeeds(theme: SpotTheme): List<StubSeed> = when (theme) {
        SpotTheme.SUNLIGHT -> listOf(
            StubSeed("${STUB_ID_PREFIX}sunlight-1", "선유도공원 잔디마당", 37.5443, 126.8963, 3.1),
            StubSeed("${STUB_ID_PREFIX}sunlight-2", "서울숲 은행나무길", 37.5445, 127.0374, 5.2),
            StubSeed("${STUB_ID_PREFIX}sunlight-3", "올림픽공원 들꽃마루", 37.5202, 127.1214, 9.8),
        )
        SpotTheme.NIGHT_VIEW -> listOf(
            StubSeed("${STUB_ID_PREFIX}night-1", "반포대교 무지개분수", 37.5127, 126.9959, 4.6),
            StubSeed("${STUB_ID_PREFIX}night-2", "낙산공원 성곽길", 37.5806, 127.0074, 2.3),
            StubSeed("${STUB_ID_PREFIX}night-3", "노들섬 야경 데크", 37.5177, 126.9583, 6.4),
        )
        // 서버가 아는 무드는 stub 을 만들지 않는다.
        SpotTheme.YUNSEUL, SpotTheme.SUNSET -> emptyList()
    }
}
