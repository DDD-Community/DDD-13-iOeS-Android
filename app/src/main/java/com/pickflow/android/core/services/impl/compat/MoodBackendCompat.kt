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
 * 2026-09-11 재실측(`/v1/spots`, `/v1/spots/viewport` 전 페이지 순회):
 *
 * | 요청 | 개발(debug) | 운영(release) |
 * |---|---|---|
 * | `?theme=SUNSET` / `?theme=YUNSEUL` | 200 | 200 |
 * | `?theme=SUNLIGHT` | **200** `{SL:1}` | **400 C002** |
 * | `?theme=NIGHT_VIEW` | **200** `{}` | **400 C002** |
 * | `?theme=A&theme=B` (반복) | **200, A∪B 정상 필터** ✅ | **200 이지만 첫 값만 적용** ⚠️ |
 * | `?theme=A,B` (CSV) | 400 | 400 |
 *
 * 즉 **개발 서버는 신규 2종과 다중 필터를 모두 지원하게 됐고, 운영 서버만 아직 옛 버전**이다.
 * (예: dev `?theme=SUNSET&theme=SUNLIGHT` → 전 페이지 합계 30건 = SS 29 + SL 1.
 * prod `?theme=SUNSET&theme=YUNSEUL` → 28건 전부 SS.)
 * 운영 쪽 반복 파라미터가 특히 위험하다 — 400이 아니라 200을 주므로 오작동을 감지할 수 없다.
 *
 * ## 무엇을 하는가
 *
 * - 서버가 아는 무드는 **실서버 응답을 그대로** 쓴다. 다중 필터를 지원하는 서버라면
 *   2개 이상 선택도 그대로 위임한다([SERVER_SUPPORTS_MULTI_THEME]).
 * - 다중을 못 받는 서버(운영)에서 2개 이상 선택되면 `theme` 없이 전체를 받아
 *   **클라이언트에서 필터**한다. 페이지네이션과 함께 쓰면 첫 페이지만 걸러지므로
 *   어디까지나 최후 수단이다.
 * - 서버가 모르는 무드(햇살/야경)는 [stubSpots] / [stubMarkers]로 채워 신규 UI를 볼 수 있게 한다.
 *   stub 스팟은 이름에 `[STUB]` 접두사가 붙어 실데이터와 구분된다.
 *
 * ## 되돌리는 법
 *
 * **부분 비활성화 스위치는 두지 않는다.** 백엔드가 준비되면 이 계층을 통째로 지우는 것이
 * 유일한 경로다 — 절차는 `docs/PV-59/backend-compat-rollback.md` 참고.
 */
object MoodBackendCompat {

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
     * 서버가 `theme` 반복 파라미터를 OR 필터로 제대로 처리하는가.
     *
     * 2026-09-11 실측으로 **개발 서버는 지원, 운영 서버는 여전히 첫 값만 적용**이다.
     * 신규 무드 지원과 같은 배포에 묶여 있어 [SERVER_KNOWN_THEMES]와 같은 기준으로 나눈다.
     */
    val SERVER_SUPPORTS_MULTI_THEME: Boolean = BuildConfig.DEBUG

    /**
     * 서버에 실제로 보낼 `theme` 집합.
     *
     * 다중을 처리하는 서버면 선택된 것 전부를 그대로 보낸다. 처리하지 못하는 서버에서
     * 2개 이상이면 빈 Set(=전체 조회)을 돌려주고 걸러내는 일은 [filterServerItems]가 맡는다.
     */
    fun serverQueryThemes(
        selected: Set<SpotTheme>,
        serverKnown: Set<SpotTheme> = SERVER_KNOWN_THEMES,
        supportsMulti: Boolean = SERVER_SUPPORTS_MULTI_THEME,
    ): Set<SpotTheme> {
        val known = selected intersect serverKnown
        return if (supportsMulti || known.size == 1) known else emptySet()
    }

    /** 서버가 아는 무드가 하나도 선택되지 않았다면 네트워크를 탈 이유가 없다. */
    fun shouldSkipNetwork(
        selected: Set<SpotTheme>,
        serverKnown: Set<SpotTheme> = SERVER_KNOWN_THEMES,
    ): Boolean = selected.isNotEmpty() && (selected intersect serverKnown).isEmpty()

    /**
     * 서버 응답을 선택된 무드로 다시 거른다.
     *
     * 서버가 필터해 준 경우엔 이미 걸러져 있어 no-op 이고,
     * 다중을 처리 못 해 전체를 받아온 경우엔 여기서 실제 필터링이 일어난다.
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
