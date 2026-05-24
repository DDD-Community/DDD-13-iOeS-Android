package com.pickflow.android.feature.spotlist.components

/**
 * iOS SpotList 스냅샷 도메인 1:1 대응 모델.
 *
 * 프로덕션 `core.services.protocols.SpotTheme`(CAFE/RESTAURANT/...)와는 별개의
 * iOS `SpotTheme`(노을/윤슬) 대응이라 이름 충돌을 피해 별도 타입으로 둔다.
 */
enum class SpotListMood(val displayName: String, val emoji: String) {
    /** iOS `.sunset` — overlay 에셋(커스텀 드로잉)은 이모지로 치환. */
    Sunset("노을", "🌅"),

    /** iOS `.reflection` — 윤슬. overlay 에셋은 이모지로 치환. */
    Reflection("윤슬", "🌊"),
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
