package com.pickflow.android.core.network.dto.region

import kotlinx.serialization.Serializable

/** `GET /v1/regions` 응답. 서버는 `regionId` 오름차순으로 준다. */
@Serializable
data class RegionListResponseDto(
    val regions: List<RegionItemDto> = emptyList(),
)

@Serializable
data class RegionItemDto(
    val regionId: Long = 0,
    val regionName: String = "",
)
