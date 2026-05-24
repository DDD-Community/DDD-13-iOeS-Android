package com.pickflow.android.feature.spotdetail.components

/**
 * iOS SpotDetail 스냅샷 도메인 1:1 대응 모델.
 *
 * 프로덕션 모델과 별개로, 스냅샷이 필요로 하는 필드만 평탄화해 담는다.
 */
enum class SpotDetailTheme(val displayName: String) {
    Sunset("노을"),
    Reflection("윤슬"),
}

/** iOS `SpotDetail.fixture()` 1:1 대응. */
data class SpotDetailData(
    val name: String = "동작구 산책로",
    val theme: SpotDetailTheme = SpotDetailTheme.Sunset,
    val comment: String = "걷다 보면 멀리 노을이 번져요.",
    val bookmarkCount: Int = 34,
    val isMine: Boolean = false,
    val isBookmarked: Boolean = false,
    val address: String = "서울 동작구",
    /** iOS `SpotDetail.distance`(km). */
    val distanceKm: Double? = 2.5,
    val hasImage: Boolean = true,
    /** iOS `pickflowDisplayTime("19:30")` 결과. */
    val recordedTime: String = "PM 7:30",
    val weatherCondition: String = "맑음",
    val precipitationProbability: Int = 15,
    /** iOS `pickflowDisplayTime("18:40")` 결과. */
    val sunsetTime: String = "PM 6:40",
    val parking: String? = "무료 주차장",
    val congestion: String = "여유",
)
