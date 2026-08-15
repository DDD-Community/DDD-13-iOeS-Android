package com.pickflow.android.feature.map

import com.pickflow.android.R
import com.pickflow.android.core.services.protocols.SpotTheme

/**
 * iOS `HomeMapView.MoodFilter` 1:1 — 지도/리스트 상단 무드 필터(햇살/윤슬/노을/야경).
 *
 * 선언 순서 = 표시 순서. `MoodFilterRow` 는 `entries` 를 그대로 순회하므로
 * 순서를 바꾸면 UI 순서가 바뀐다(별도 정렬 로직 없음).
 */
enum class MoodFilter(val displayName: String, val iconRes: Int) {
    Sunlight("햇살", R.drawable.ic_sunny),
    Reflection("윤슬", R.drawable.ic_reflection),
    Sunset("노을", R.drawable.ic_sunset),
    Night("야경", R.drawable.ic_night),
}

/** UI 무드 → 도메인 테마. 지도·리스트가 공유한다(각 화면에서 재정의 금지). */
fun MoodFilter.toTheme(): SpotTheme = when (this) {
    MoodFilter.Sunlight -> SpotTheme.SUNLIGHT
    MoodFilter.Reflection -> SpotTheme.YUNSEUL
    MoodFilter.Sunset -> SpotTheme.SUNSET
    MoodFilter.Night -> SpotTheme.NIGHT_VIEW
}

/** 도메인 테마 → UI 무드. */
fun SpotTheme.toMood(): MoodFilter = when (this) {
    SpotTheme.SUNLIGHT -> MoodFilter.Sunlight
    SpotTheme.YUNSEUL -> MoodFilter.Reflection
    SpotTheme.SUNSET -> MoodFilter.Sunset
    SpotTheme.NIGHT_VIEW -> MoodFilter.Night
}
