package com.pickflow.android.core.services.protocols

/**
 * 검색/역지오코딩 결과 1건. iOS `Address` 1:1 축약.
 * - name: 장소명(placeName). 좌표 검색 결과는 도로명 주소를 name 으로 사용.
 * - fullAddress: 표시용 전체 주소(도로명 우선, 없으면 지번).
 */
data class AddressSuggestion(
    val name: String,
    val fullAddress: String,
    val latitude: Double,
    val longitude: Double,
) {
    /** 리스트/하위 호환용 표시명. */
    val displayName: String get() = name
}

interface AddressService {
    suspend fun search(query: String): List<AddressSuggestion>

    /** 좌표 → 주소(coord2address). 핀 이동 후 정확한 주소 갱신용. iOS `reverseGeocode` 1:1. */
    suspend fun reverseGeocode(latitude: Double, longitude: Double): AddressSuggestion?
}
