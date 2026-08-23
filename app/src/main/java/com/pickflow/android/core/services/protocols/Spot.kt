package com.pickflow.android.core.services.protocols

data class Spot(
    val id: String,
    val name: String,
    val theme: SpotTheme,
    val latitude: Double,
    val longitude: Double,
    val imageUrl: String? = null,
    val address: String = "",
    val distanceKm: Double? = null,
    val isBookmarked: Boolean = false,
)

/**
 * 서버 스펙(OpenAPI Photo API v1.0.0)과 정렬된 테마 enum.
 * - SUNLIGHT: 햇살
 * - YUNSEUL: 윤슬
 * - SUNSET: 노을
 * - NIGHT: 야경
 *
 * 선언 순서 = UI 표시 순서(햇살/윤슬/노을/야경). 무드 필터·등록 칩은 `entries`를 그대로
 * 순회하므로 별도 정렬 로직을 두지 않는다. 순서를 바꾸면 UI 순서가 바뀐다.
 *
 * 서버 전송값은 `name` 그대로 쓴다. SUNLIGHT/NIGHT 는 PV-59 백엔드 확정시 변경 가능성 있음.
 */
enum class SpotTheme { SUNLIGHT, YUNSEUL, SUNSET, NIGHT_VIEW }

data class SpotPage(
    val items: List<Spot>,
    val page: Int,
    val hasNext: Boolean,
)
