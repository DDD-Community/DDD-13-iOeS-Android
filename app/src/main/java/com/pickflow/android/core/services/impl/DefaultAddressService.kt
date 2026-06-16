package com.pickflow.android.core.services.impl

import com.pickflow.android.BuildConfig
import com.pickflow.android.core.network.api.KakaoLocalApi
import com.pickflow.android.core.services.protocols.AddressService
import com.pickflow.android.core.services.protocols.AddressSuggestion
import javax.inject.Inject
import javax.inject.Named

/**
 * iOS `AddressService.swift` 1:1 — Kakao Local API 직접 호출.
 *
 * `BuildConfig.KAKAO_REST_API_KEY` 가 비어 있으면 모든 호출이 401 → 빈 결과로 폴백.
 * iOS 도 동일하게 키 누락 시 인증 실패 → 사용자 보임 결과 없음.
 */
class DefaultAddressService @Inject constructor(
    private val api: KakaoLocalApi,
    @Named("kakaoRestApiKey") private val restApiKey: String,
) : AddressService {

    override suspend fun search(query: String): List<AddressSuggestion> {
        if (query.isBlank() || restApiKey.isBlank()) return emptyList()
        val authHeader = "KakaoAK $restApiKey"
        val response = runCatching { api.searchKeyword(authHeader, query) }
            .getOrNull() ?: return emptyList()
        return response.documents.mapNotNull { doc ->
            val lat = doc.y.toDoubleOrNull() ?: return@mapNotNull null
            val lon = doc.x.toDoubleOrNull() ?: return@mapNotNull null
            AddressSuggestion(
                displayName = doc.placeName.ifBlank { doc.addressName },
                latitude = lat,
                longitude = lon,
            )
        }
    }
}
