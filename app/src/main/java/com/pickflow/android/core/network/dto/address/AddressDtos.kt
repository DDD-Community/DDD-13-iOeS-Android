package com.pickflow.android.core.network.dto.address

import kotlinx.serialization.Serializable

/** `GET /v1/address/search` 응답. 서버가 Kakao 를 대신 호출한다. */
@Serializable
data class AddressSearchResponseDto(
    val addresses: List<AddressItemDto> = emptyList(),
    val page: Int = 1,
    val totalCount: Int = 0,
    val isEnd: Boolean = true,
)

@Serializable
data class AddressItemDto(
    /** 전체 주소(대표). 도로명이 있으면 도로명. */
    val addressName: String = "",
    val roadAddress: String? = null,
    val jibunAddress: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
)
