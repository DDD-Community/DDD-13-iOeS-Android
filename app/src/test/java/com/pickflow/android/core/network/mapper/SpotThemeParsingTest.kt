package com.pickflow.android.core.network.mapper

import com.pickflow.android.core.services.protocols.SpotTheme
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * 서버 `theme` 계약을 코드로 고정한다 (2026-08-18 실측 + BE PR #162 확인).
 *
 * **요청은 풀네임, 응답은 엔드포인트마다 다르다.**
 *
 * | 방향 | 엔드포인트 | 형식 |
 * |---|---|---|
 * | 요청 | `GET /v1/spots`, `GET /v1/spots/viewport` | 풀네임 `SUNSET` `YUNSEUL` `SUNLIGHT` `NIGHT_VIEW` |
 * | 응답 | `GET /v1/spots` (리스트) | **2글자 코드** `SS` `YS` `SL` `NV` |
 * | 응답 | `GET /v1/spots/{id}`, `.../preview` | 풀네임 |
 *
 * 요청값은 `SpotTheme.name` 을 그대로 쓰므로 **enum 상수명을 바꾸면 API 계약이 바뀐다.**
 * 응답은 두 형식이 섞여 오므로 [parseTheme] 이 양쪽을 모두 받는다.
 */
class SpotThemeParsingTest {

    /** 요청 방향 — enum 이름이 곧 서버 전송값이다. 이름을 바꾸면 이 테스트가 깨진다. */
    @Test
    fun `request values are the full enum names accepted by the server`() {
        assertEquals(
            listOf("SUNLIGHT", "YUNSEUL", "SUNSET", "NIGHT_VIEW"),
            SpotTheme.entries.map { it.name },
        )
    }

    /** 응답 방향 — 리스트는 2글자 코드, 상세/미리보기는 풀네임으로 온다. */
    @Test
    fun `parses both short codes and full names`() {
        // 2글자 코드 — GET /v1/spots 응답
        assertEquals(SpotTheme.SUNLIGHT, parseTheme("SL"))
        assertEquals(SpotTheme.YUNSEUL, parseTheme("YS"))
        assertEquals(SpotTheme.SUNSET, parseTheme("SS"))
        assertEquals(SpotTheme.NIGHT_VIEW, parseTheme("NV"))
        // 풀네임 — GET /v1/spots/{id}, /preview 응답
        assertEquals(SpotTheme.SUNLIGHT, parseTheme("SUNLIGHT"))
        assertEquals(SpotTheme.YUNSEUL, parseTheme("YUNSEUL"))
        assertEquals(SpotTheme.SUNSET, parseTheme("SUNSET"))
        assertEquals(SpotTheme.NIGHT_VIEW, parseTheme("NIGHT_VIEW"))
    }

    /** 4종 전부 왕복(요청값 → 응답 코드 → 도메인)이 자기 자신으로 돌아온다. */
    @Test
    fun `every mood round trips through both response formats`() {
        val shortCodes = mapOf(
            SpotTheme.SUNLIGHT to "SL",
            SpotTheme.YUNSEUL to "YS",
            SpotTheme.SUNSET to "SS",
            SpotTheme.NIGHT_VIEW to "NV",
        )
        SpotTheme.entries.forEach { theme ->
            assertEquals(theme, parseTheme(theme.name), "풀네임 응답")
            assertEquals(theme, parseTheme(shortCodes.getValue(theme)), "2글자 코드 응답")
        }
    }

    @Test
    fun `parses case insensitively`() {
        assertEquals(SpotTheme.SUNLIGHT, parseTheme("sunlight"))
        assertEquals(SpotTheme.NIGHT_VIEW, parseTheme("night_view"))
    }

    @Test
    fun `falls back to SUNSET for unknown values`() {
        assertEquals(SpotTheme.SUNSET, parseTheme("WHATEVER"))
        assertEquals(SpotTheme.SUNSET, parseTheme(""))
    }

    @Test
    fun `declaration order drives the mood filter display order`() {
        // 무드 행·등록 칩은 entries 를 그대로 순회한다. 순서가 곧 UI 순서다.
        assertEquals(
            listOf(SpotTheme.SUNLIGHT, SpotTheme.YUNSEUL, SpotTheme.SUNSET, SpotTheme.NIGHT_VIEW),
            SpotTheme.entries,
        )
    }
}
