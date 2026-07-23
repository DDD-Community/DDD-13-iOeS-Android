package com.pickflow.android.feature.spotdetail.components

import com.pickflow.android.core.services.protocols.CongestionLevel
import com.pickflow.android.core.services.protocols.Precipitation
import com.pickflow.android.core.services.protocols.SpotDetail
import com.pickflow.android.core.services.protocols.SpotTheme
import com.pickflow.android.core.services.protocols.SpotWeather
import com.pickflow.android.core.services.protocols.WeatherSky

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
    /** 펼침 시 표시할 도로명 주소(없으면 null). */
    val addressRoad: String? = null,
    /** 펼침 시 표시할 지번 주소(없으면 null). */
    val addressJibun: String? = null,
    /** iOS `SpotDetail.distance`(km). */
    val distanceKm: Double? = 2.5,
    val hasImage: Boolean = true,
    /** 서버 `imageUrl` — 사진 박스에 표시할 원본 이미지 URL. */
    val imageUrl: String? = null,
    /** 사진 좌상단 촬영 기록 배지 — "yy.MM.dd AM/PM h:mm". */
    val recordedTime: String = "26.05.25 PM 7:30",
    val weatherCondition: String = "맑음",
    val precipitationProbability: Int = 15,
    /** iOS `pickflowDisplayTime("18:40")` 결과. */
    val sunsetTime: String = "PM 6:40",
    val parking: String? = "무료 주차장",
    val congestion: String = "여유",
)

/**
 * 도메인 `SpotDetail` → 표시용 `SpotDetailData` 평탄화.
 *
 * iOS `SpotDetailView`가 `SpotDetail` 도메인 + `pickflowDisplayTime` 헬퍼로
 * 즉석에서 만드는 값을 동일 자리에서 한 번에 만든다.
 */
fun SpotDetail.toDetailData(isBookmarked: Boolean): SpotDetailData =
    SpotDetailData(
        name = name,
        theme = theme.toDetailTheme(),
        comment = comment,
        bookmarkCount = bookmarkCount.toInt(),
        isMine = isMySpot,
        isBookmarked = isBookmarked,
        address = address,
        distanceKm = null,
        hasImage = imageUrl?.isNotBlank() == true,
        imageUrl = imageUrl,
        recordedTime = recordedBadgeText(recordedDate, recordedTime),
        weatherCondition = weather?.toDisplayName() ?: "-",
        precipitationProbability = weather?.precipitationProbability ?: 0,
        sunsetTime = sunsetTime?.let(::pickflowDisplayTime) ?: "-",
        parking = parkingInfo,
        congestion = congestion?.level?.toDisplayName() ?: "-",
    )

private fun SpotTheme.toDetailTheme(): SpotDetailTheme = when (this) {
    SpotTheme.SUNSET -> SpotDetailTheme.Sunset
    SpotTheme.YUNSEUL -> SpotDetailTheme.Reflection
}

/**
 * 서버 `recordedDate`("yyyy-MM-dd") + `recordedTime`("HH:mm") →
 * "yy.MM.dd AM/PM h:mm" 배지 문자열. 시각은 [pickflowDisplayTime] 변환.
 * 파싱 불가 값은 원문을 그대로 이어 붙인다.
 */
internal fun recordedBadgeText(recordedDate: String, recordedTime: String): String {
    val datePart = recordedDate.split("-")
        .takeIf { it.size == 3 && it[0].length == 4 }
        ?.let { (y, m, d) -> "${y.takeLast(2)}.$m.$d" }
        ?: recordedDate
    return listOf(datePart, pickflowDisplayTime(recordedTime))
        .filter { it.isNotBlank() }
        .joinToString(" ")
}

/** iOS `DateFormatter.pickflowDisplayTime` 1:1 — "HH:mm" → "AM/PM h:mm". 파싱 불가 시 원문. */
internal fun pickflowDisplayTime(time: String): String =
    time.split(":")
        .mapNotNull { it.toIntOrNull() }
        .takeIf { it.size == 2 }
        ?.let { (hour, minute) ->
            val period = if (hour < 12) "AM" else "PM"
            val displayHour = if (hour % 12 == 0) 12 else hour % 12
            "%s %d:%02d".format(period, displayHour, minute)
        }
        ?: time

private fun SpotWeather.toDisplayName(): String {
    return when (precipitation) {
        Precipitation.RAIN -> "비"
        Precipitation.RAIN_SNOW -> "비/눈"
        Precipitation.SNOW -> "눈"
        Precipitation.SHOWER -> "소나기"
        Precipitation.NONE -> when (sky) {
            WeatherSky.CLEAR -> "맑음"
            WeatherSky.MOSTLY_CLOUDY -> "구름많음"
            WeatherSky.OVERCAST -> "흐림"
        }
    }
}

private fun CongestionLevel.toDisplayName(): String = when (this) {
    CongestionLevel.RELAXED -> "여유"
    CongestionLevel.NORMAL -> "보통"
    CongestionLevel.SLIGHTLY_CROWDED -> "약간 붐빔"
    CongestionLevel.CROWDED -> "붐빔"
}
