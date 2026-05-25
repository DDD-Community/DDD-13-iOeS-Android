package com.pickflow.android.core.services.protocols

/**
 * 지도 뷰포트 영역의 스팟 마커들을 조회한다. (GET /v1/spots/viewport 대응)
 *
 * 클러스터링과 무관하게 서버에서 단순 좌표/이미지/isMySpot만 받아 마커 표시에 사용.
 */
interface SpotMapService {
    suspend fun fetchInViewport(box: ViewportBox, theme: SpotTheme? = null): List<SpotMapMarker>
}

data class ViewportBox(
    val topLeft: Coordinates,
    val topRight: Coordinates,
    val bottomLeft: Coordinates,
    val bottomRight: Coordinates,
)

data class SpotMapMarker(
    val spotId: Long,
    val imageUrl: String?,
    val coordinates: Coordinates,
    val isMySpot: Boolean,
)
