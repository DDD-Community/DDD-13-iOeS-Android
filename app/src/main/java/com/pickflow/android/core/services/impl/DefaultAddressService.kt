package com.pickflow.android.core.services.impl

import com.pickflow.android.core.network.api.AddressApi
import com.pickflow.android.core.network.api.KakaoLocalApi
import com.pickflow.android.core.services.protocols.AddressService
import com.pickflow.android.core.services.protocols.AddressSuggestion
import javax.inject.Inject
import javax.inject.Named

/** 서버 `size` 상한. 한 화면에 다 담기므로 페이징은 쓰지 않는다. */
private const val SEARCH_SIZE = 30

/**
 * 검색은 서버 `GET /v1/address/search`(Kakao 를 서버가 대신 호출), 역지오코딩은 Kakao Local 직접 호출.
 *
 * 역지오코딩(coord2address)만 서버에 대응 엔드포인트가 없어 `KAKAO_REST_API_KEY` 를 아직 쓴다.
 * 키가 비어 있으면 역지오코딩은 null 폴백.
 */
class DefaultAddressService @Inject constructor(
    private val addressApi: AddressApi,
    private val kakaoApi: KakaoLocalApi,
    @Named("kakaoRestApiKey") private val restApiKey: String,
) : AddressService {

    override suspend fun search(query: String): List<AddressSuggestion> {
        if (query.isBlank()) return emptyList()
        val response = runCatching { addressApi.searchAddress(query, SEARCH_SIZE) }
            .getOrNull()
            ?.takeIf { it.success }
            ?.data
            ?: return emptyList()
        return response.addresses.mapNotNull { item ->
            val lat = item.latitude ?: return@mapNotNull null
            val lon = item.longitude ?: return@mapNotNull null
            val full = item.addressName.ifBlank { item.roadAddress.orEmpty() }
                .ifBlank { item.jibunAddress.orEmpty() }
                .ifBlank { return@mapNotNull null }
            // 서버 응답에 장소명이 없다. 역지오코딩과 같이 주소를 이름으로 쓴다.
            AddressSuggestion(name = full, fullAddress = full, latitude = lat, longitude = lon)
        }
    }

    override suspend fun reverseGeocode(latitude: Double, longitude: Double): AddressSuggestion? {
        if (restApiKey.isBlank()) return null
        val response = runCatching {
            kakaoApi.coord2Address(authHeader(), longitude.toString(), latitude.toString())
        }.getOrNull() ?: return null
        val doc = response.documents.firstOrNull() ?: return null
        val road = doc.roadAddress?.addressName?.takeIf { it.isNotBlank() }
        val jibun = doc.address?.addressName?.takeIf { it.isNotBlank() }
        val full = road ?: jibun ?: return null
        return AddressSuggestion(
            name = full,
            fullAddress = full,
            latitude = latitude,
            longitude = longitude,
        )
    }

    private fun authHeader() = "KakaoAK $restApiKey"
}
