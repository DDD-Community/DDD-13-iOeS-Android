package com.pickflow.android.feature.map

import com.pickflow.android.core.services.protocols.Coordinates

/**
 * 클러스터링 미참여 마이스팟 단일 마커.
 *
 * iOS `MySpot`(Feature/Map) 대응 — `/v1/spots/viewport` 응답의 `isMySpot=true`
 * 항목만 분기해 [HomeMapViewModel.mySpots] 에 모인다. NaverMap clusterer 외부에서
 * 별도 `Marker` 로 add/remove 된다.
 */
data class MySpotMarker(
    val spotId: Long,
    val coordinates: Coordinates,
    val imageUrl: String?,
)
