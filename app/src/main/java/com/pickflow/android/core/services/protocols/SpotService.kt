package com.pickflow.android.core.services.protocols

interface SpotService {
    suspend fun spot(id: String): SpotDetail
    suspend fun preview(id: String, coordinates: Coordinates? = null): SpotPreview
    suspend fun register(draft: SpotDraft): Spot

    /** 스팟 추천(좋아요) 등록/해제. 갱신된 추천 수는 화면에서 쓰지 않아 반환하지 않는다. */
    suspend fun like(id: String)
    suspend fun unlike(id: String)
}

data class SpotDraft(
    val name: String,
    val theme: SpotTheme,
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val capturedDate: String = "",
    val capturedTime: String = "",
    val comment: String = "",
    val imageUrl: String? = null,
)
