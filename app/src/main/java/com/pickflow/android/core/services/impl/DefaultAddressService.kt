package com.pickflow.android.core.services.impl

import com.pickflow.android.core.network.api.KakaoLocalApi
import com.pickflow.android.core.services.protocols.AddressService
import com.pickflow.android.core.services.protocols.AddressSuggestion
import javax.inject.Inject
import javax.inject.Named

/**
 * iOS `AddressService.swift` 1:1 — Kakao Local API 직접 호출.
 *
 * `KAKAO_REST_API_KEY` 가 비어 있으면 모든 호출이 인증 실패 → 빈 결과로 폴백.
 */
class DefaultAddressService @Inject constructor(
    private val api: KakaoLocalApi,
    @Named("kakaoRestApiKey") private val restApiKey: String,
) : AddressService {

    override suspend fun search(query: String): List<AddressSuggestion> {
        if (query.isBlank() || restApiKey.isBlank()) return emptyList()
        val response = runCatching { api.searchKeyword(authHeader(), query) }
            .getOrNull() ?: return emptyList()
        return response.documents.mapNotNull { doc ->
            val lat = doc.y.toDoubleOrNull() ?: return@mapNotNull null
            val lon = doc.x.toDoubleOrNull() ?: return@mapNotNull null
            AddressSuggestion(
                name = doc.placeName.ifBlank { doc.roadAddressName ?: doc.addressName },
                fullAddress = (doc.roadAddressName ?: doc.addressName).ifBlank { doc.placeName },
                latitude = lat,
                longitude = lon,
            )
        }
    }

    override suspend fun reverseGeocode(latitude: Double, longitude: Double): AddressSuggestion? {
        if (restApiKey.isBlank()) return null
        val response = runCatching {
            api.coord2Address(authHeader(), longitude.toString(), latitude.toString())
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
