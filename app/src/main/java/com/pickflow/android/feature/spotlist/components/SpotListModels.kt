package com.pickflow.android.feature.spotlist.components

import com.pickflow.android.R

/**
 * iOS SpotList 스냅샷 도메인 1:1 대응 모델.
 *
 * 프로덕션 `core.services.protocols.SpotTheme`(CAFE/RESTAURANT/...)와는 별개의
 * iOS `SpotTheme`(노을/윤슬) 대응이라 이름 충돌을 피해 별도 타입으로 둔다.
 */
enum class SpotListMood(val displayName: String, val iconRes: Int) {
    /** iOS `.sunset` — 탐색 탭 노을 필터와 동일한 아이콘 사용. */
    Sunset("노을", R.drawable.ic_sunset),

    /** iOS `.reflection` — 탐색 탭 윤슬 필터와 동일한 아이콘 사용. */
    Reflection("윤슬", R.drawable.ic_reflection),
}

/** iOS `SpotListItem` 1:1 대응. */
data class SpotListGridItem(
    val spotId: Long,
    val name: String,
    val mood: SpotListMood,
    val hasThumbnail: Boolean = true,
    val distanceKm: Double? = 1.2,
)

/** iOS `SpotListSort` 1:1 대응. */
enum class SpotListSortOption(val displayName: String) {
    Nearest("가까운 순"),
    Bookmark("북마크 순"),
}
