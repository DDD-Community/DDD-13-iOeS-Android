package com.pickflow.android.feature.map

import com.pickflow.android.core.services.protocols.SpotTheme
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** 무드 필터의 표시 순서·라벨·도메인 매핑. */
class MoodFilterTest {

    @Test
    fun `entries are ordered 햇살 윤슬 노을 야경`() {
        assertEquals(
            listOf("햇살", "윤슬", "노을", "야경"),
            MoodFilter.entries.map { it.displayName },
        )
    }

    @Test
    fun `maps to domain theme and back without loss`() {
        MoodFilter.entries.forEach { mood ->
            assertEquals(mood, mood.toTheme().toMood())
        }
        SpotTheme.entries.forEach { theme ->
            assertEquals(theme, theme.toMood().toTheme())
        }
    }

    @Test
    fun `mood order matches domain theme order`() {
        assertEquals(SpotTheme.entries.map { it.toMood() }, MoodFilter.entries)
    }
}
