package com.pickflow.android.core.network.mapper

import com.pickflow.android.core.services.protocols.SpotTheme
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * `parseTheme` — 서버가 2글자 코드/풀네임 어느 쪽으로 응답해도 같은 도메인 값으로 접힌다.
 * 요청 enum 은 풀네임(SUNSET/YUNSEUL/SUNLIGHT/NIGHT_VIEW), 응답은 2글자 코드(SS/YS)다.
 * 신규 2종의 2글자 코드(SL/NV)는 문서 미기재 — 추정값이다.
 */
class SpotThemeParsingTest {

    @Test
    fun `parses both short codes and full names`() {
        assertEquals(SpotTheme.SUNLIGHT, parseTheme("SL"))
        assertEquals(SpotTheme.SUNLIGHT, parseTheme("SUNLIGHT"))
        assertEquals(SpotTheme.YUNSEUL, parseTheme("YS"))
        assertEquals(SpotTheme.YUNSEUL, parseTheme("YUNSEUL"))
        assertEquals(SpotTheme.SUNSET, parseTheme("SS"))
        assertEquals(SpotTheme.SUNSET, parseTheme("SUNSET"))
        assertEquals(SpotTheme.NIGHT_VIEW, parseTheme("NV"))
        assertEquals(SpotTheme.NIGHT_VIEW, parseTheme("NIGHT_VIEW"))
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
