package com.pickflow.android.core.network.dto.region

import kotlinx.serialization.Serializable

/** `GET /v1/regions` 응답. 순서는 보장하지 않고, 앱이 `regionId` 오름차순으로 정규화한다. */
@Serializable
data class RegionListResponseDto(
    val regions: List<RegionItemDto> = emptyList(),
)

@Serializable
data class RegionItemDto(
    val regionId: Long = 0,
    val regionName: String = "",
)
