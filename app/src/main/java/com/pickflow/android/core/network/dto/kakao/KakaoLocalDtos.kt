package com.pickflow.android.core.network.dto.kakao

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** iOS `KakaoCoord2AddressResponse` 1:1. */
@Serializable
data class KakaoCoord2AddressResponse(
    val documents: List<KakaoCoord2AddressDocument> = emptyList(),
)

@Serializable
data class KakaoCoord2AddressDocument(
    @SerialName("road_address") val roadAddress: KakaoCoord2RoadAddress? = null,
    val address: KakaoCoord2Address? = null,
)

@Serializable
data class KakaoCoord2RoadAddress(
    @SerialName("address_name") val addressName: String = "",
    @SerialName("region_1depth_name") val region1depthName: String? = null,
    @SerialName("region_2depth_name") val region2depthName: String? = null,
    @SerialName("zone_no") val zoneNo: String? = null,
)

@Serializable
data class KakaoCoord2Address(
    @SerialName("address_name") val addressName: String = "",
    @SerialName("region_1depth_name") val region1depthName: String? = null,
    @SerialName("region_2depth_name") val region2depthName: String? = null,
)
