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
)

fun SpotListResponseDto.toSpotPage(): SpotPage = SpotPage(
    items = spots.map { it.toSpot() },
    page = page,
    hasNext = hasNext,
)

// 서버는 endpoint 에 따라 2글자 코드("YS"/"SS") 또는 풀네임("YUNSEUL"/"SUNSET")으로 응답한다.
// SL/SUNLIGHT(햇살), NT/NIGHT(야경) 코드는 PV-59 백엔드 확정시 변경 가능성 있음.
internal fun parseTheme(value: String): SpotTheme = when (value.uppercase()) {
    "YS", "YUNSEUL" -> SpotTheme.YUNSEUL
    "SS", "SUNSET" -> SpotTheme.SUNSET
    "SL", "SUNLIGHT" -> SpotTheme.SUNLIGHT
    "NT", "NIGHT" -> SpotTheme.NIGHT
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
)
