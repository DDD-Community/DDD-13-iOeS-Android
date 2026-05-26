package com.pickflow.android.feature.map

import com.pickflow.android.R

/**
 * iOS `HomeMapView.MoodFilter` 1:1 — 지도 상단 무드 필터(노을/윤슬).
 */
enum class MoodFilter(val displayName: String, val iconRes: Int) {
    Sunset("노을", R.drawable.ic_sunset),
    Reflection("윤슬", R.drawable.ic_reflection),
}
