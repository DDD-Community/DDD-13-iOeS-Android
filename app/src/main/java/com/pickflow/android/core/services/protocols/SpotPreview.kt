package com.pickflow.android.core.services.protocols

/**
 * GET /v1/spots/{spotId}/preview 도메인 모델.
 *
 * 지도 마커 탭, 리스트 호버 등 "상세 진입 전 간략 정보" 표시용.
 * - distanceKm: 위치 미전달 시 null
 * - imageUrl: 빈 문자열은 null로 정규화
 * - addressRoad/addressJibun: 서버에서 주소 스키마 재구성 전까지 null 반환 가능
 * - isMySpot: 비로그인 시 항상 false
 */
data class SpotPreview(
    val id: Long,
    val name: String,
    val isMySpot: Boolean,
    val theme: SpotTheme,
    val bookmarkCount: Long,
    val distanceKm: Double?,
    val imageUrl: String?,
    val addressSimple: String,
    val addressRoad: String?,
    val addressJibun: String?,
)
