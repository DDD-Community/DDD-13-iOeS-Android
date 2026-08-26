package com.pickflow.android.core.network.mapper

import com.pickflow.android.core.network.dto.spot.SpotDetailResponseDto
import com.pickflow.android.core.network.dto.spot.SpotItemDto
import com.pickflow.android.core.network.dto.spot.SpotListResponseDto
import com.pickflow.android.core.network.dto.spot.SpotPreviewResponseDto
import com.pickflow.android.core.network.dto.spot.SpotSummaryDto
import com.pickflow.android.core.services.protocols.CongestionLevel
import com.pickflow.android.core.services.protocols.Coordinates
import com.pickflow.android.core.services.protocols.Precipitation
import com.pickflow.android.core.services.protocols.Spot
import com.pickflow.android.core.services.protocols.SpotCongestion
import com.pickflow.android.core.services.protocols.SpotDetail
import com.pickflow.android.core.services.protocols.SpotMapMarker
import com.pickflow.android.core.services.protocols.SpotPage
import com.pickflow.android.core.services.protocols.SpotPreview
import com.pickflow.android.core.services.protocols.SpotTheme
import com.pickflow.android.core.services.protocols.SpotWeather
import com.pickflow.android.core.services.protocols.WeatherSky

fun SpotSummaryDto.toMapMarker(): SpotMapMarker = SpotMapMarker(
    spotId = spotId,
    imageUrl = spotImageUrl?.takeIf { it.isNotBlank() },
    coordinates = Coordinates(latitude = latitude, longitude = longitude),
    isMySpot = isMySpot,
)

fun SpotItemDto.toSpot(): Spot = Spot(
    id = spotId.toString(),
    name = name,
    theme = parseTheme(theme),
    latitude = 0.0,
    longitude = 0.0,
    imageUrl = thumbnailUrl?.takeIf { it.isNotBlank() },
    address = "",
    distanceKm = distanceKm,
    bookmarkCount = bookmarkCount,
    isBookmarked = isBookmarked,
    likeCount = likeCount,
    isLiked = isLiked,
)

fun SpotListResponseDto.toSpotPage(): SpotPage = SpotPage(
    items = spots.map { it.toSpot() },
    page = page,
    hasNext = hasNext,
)

/**
 * 서버 `theme` 문자열 → 도메인 [SpotTheme].
 *
 * **요청은 풀네임, 응답은 엔드포인트마다 형식이 다르다** (2026-08-18 실측 + BE PR #162):
 *
 * | 방향 | 엔드포인트 | 형식 | 예 |
 * |---|---|---|---|
 * | 요청 | `/v1/spots`, `/v1/spots/viewport` | 풀네임 | `?theme=SUNSET&theme=YUNSEUL` |
 * | 응답 | `/v1/spots` (리스트) | 2글자 코드 | `"SS"` |
 * | 응답 | `/v1/spots/{id}`, `.../preview` | 풀네임 | `"YUNSEUL"` |
 *
 * 대응: 노을 `SS` · 윤슬 `YS` · 햇살 `SL` · 야경 `NV`.
 * 서버 내부에서 `SpotTheme.getCode()` 가 만드는 값이라 요청에는 쓸 수 없다(400 C002).
 *
 * `"NIGHT"` 은 야경 코드가 `NIGHT_VIEW` 로 확정되기 전의 값이라 관용적으로 함께 받는다.
 */
internal fun parseTheme(value: String): SpotTheme = when (value.uppercase()) {
    "YS", "YUNSEUL" -> SpotTheme.YUNSEUL
    "SS", "SUNSET" -> SpotTheme.SUNSET
    "SL", "SUNLIGHT" -> SpotTheme.SUNLIGHT
    "NV", "NIGHT_VIEW", "NIGHT" -> SpotTheme.NIGHT_VIEW
    else -> SpotTheme.SUNSET
}

fun SpotDetailResponseDto.toSpotDetail(): SpotDetail = SpotDetail(
    id = spotId,
    name = name,
    comment = comment,
    theme = parseTheme(theme),
    latitude = latitude,
    longitude = longitude,
    address = address,
    addressRoad = addressRoad?.takeIf { it.isNotBlank() },
    addressJibun = addressJibun?.takeIf { it.isNotBlank() },
    imageUrl = imageUrl?.takeIf { it.isNotBlank() },
    recordedDate = recordedDate,
    recordedTime = recordedTime,
    weather = weatherSky?.let { sky ->
        SpotWeather(
            sky = parseSky(sky),
            precipitation = parsePrecipitation(precipitation ?: "NONE"),
            precipitationProbability = precipitationProbability,
        )
    },
    congestion = congestionLevel?.let { SpotCongestion(level = parseCongestion(it)) },
    sunsetTime = sunsetTime?.takeIf { it.isNotBlank() },
    astronomyDate = astronomyDate?.takeIf { it.isNotBlank() },
    weatherUpdatedAt = weatherUpdatedAt?.takeIf { it.isNotBlank() },
    congestionUpdatedAt = congestionUpdatedAt?.takeIf { it.isNotBlank() },
    parkingInfo = parkingInfo?.takeIf { it.isNotBlank() },
    bookmarkCount = bookmarkCount,
    isBookmarked = isBookmarked,
    isMySpot = isMySpot,
    likeCount = likeCount,
    isLiked = isLiked,
    isLikeable = isLikeable,
)

internal fun parseSky(value: String): WeatherSky = when (value.uppercase()) {
    "MOSTLY_CLOUDY" -> WeatherSky.MOSTLY_CLOUDY
    "OVERCAST" -> WeatherSky.OVERCAST
    else -> WeatherSky.CLEAR
}

internal fun parsePrecipitation(value: String): Precipitation = when (value.uppercase()) {
    "RAIN" -> Precipitation.RAIN
    "RAIN_SNOW" -> Precipitation.RAIN_SNOW
    "SNOW" -> Precipitation.SNOW
    "SHOWER" -> Precipitation.SHOWER
    else -> Precipitation.NONE
}

internal fun parseCongestion(value: String): CongestionLevel = when (value.uppercase()) {
    "NORMAL" -> CongestionLevel.NORMAL
    "SLIGHTLY_CROWDED" -> CongestionLevel.SLIGHTLY_CROWDED
    "CROWDED" -> CongestionLevel.CROWDED
    else -> CongestionLevel.RELAXED
}

fun SpotPreviewResponseDto.toSpotPreview(): SpotPreview = SpotPreview(
    id = spotId,
    name = name,
    isMySpot = isMySpot,
    theme = parseTheme(theme),
    bookmarkCount = bookmarkCount,
    distanceKm = distanceKm,
    imageUrl = imageUrl?.takeIf { it.isNotBlank() },
    addressSimple = addressSimple,
    addressRoad = addressRoad?.takeIf { it.isNotBlank() },
    addressJibun = addressJibun?.takeIf { it.isNotBlank() },
    isBookmarked = isBookmarked,
)
