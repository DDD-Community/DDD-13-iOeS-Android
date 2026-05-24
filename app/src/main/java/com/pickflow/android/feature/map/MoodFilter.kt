package com.pickflow.android.feature.map

/**
 * iOS `HomeMapView.MoodFilter` 1:1 — 지도 상단 무드 필터(노을/윤슬).
 *
 * iOS는 커스텀 이미지 에셋(`sunset`/`sparklingRipple`)을 쓰지만, 에셋 치환 규칙대로
 * 이모지 placeholder로 자리만 잡는다.
 */
enum class MoodFilter(val displayName: String, val emoji: String) {
    Sunset("노을", "🌅"),
    Reflection("윤슬", "🌊"),
}
