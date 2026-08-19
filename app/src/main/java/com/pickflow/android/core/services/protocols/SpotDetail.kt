package com.pickflow.android.core.services.protocols

/**
 * GET /v1/spots/{spotId} 도메인 모델. 서버 22필드를 클라이언트 친화적으로 묶었다.
 *
 * - id: 서버 Long. (`Spot.id: String`은 별도 — 향후 통일 마이그레이션 예정)
 * - weather/congestion: 서버에서 null 가능 → 데이터 없을 때 섹션 미렌더링
 * - isBookmarked/isMySpot: 비로그인 시 false
 */
data class SpotDetail(
    val id: Long,
    val name: String,
    val comment: String,
    val theme: SpotTheme,
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val addressRoad: String?,
    val addressJibun: String?,
    val imageUrl: String?,
    val recordedDate: String,
    val recordedTime: String,
    val weather: SpotWeather?,
    val congestion: SpotCongestion?,
    val sunsetTime: String?,
    val astronomyDate: String?,
    val weatherUpdatedAt: String?,
    val congestionUpdatedAt: String?,
    val parkingInfo: String?,
    val bookmarkCount: Long,
    val isBookmarked: Boolean,
    val isMySpot: Boolean,
    val likeCount: Long = 0L,
    val isLiked: Boolean = false,
    /** 추천 버튼 노출 여부. 서버가 내려주는 값을 그대로 따른다. */
    val isLikeable: Boolean = false,
)

data class SpotWeather(
    val sky: WeatherSky,
    val precipitation: Precipitation,
    val precipitationProbability: Int,
)

data class SpotCongestion(
    val level: CongestionLevel,
)

enum class WeatherSky { CLEAR, MOSTLY_CLOUDY, OVERCAST }
enum class Precipitation { NONE, RAIN, RAIN_SNOW, SNOW, SHOWER }
enum class CongestionLevel { RELAXED, NORMAL, SLIGHTLY_CROWDED, CROWDED }
