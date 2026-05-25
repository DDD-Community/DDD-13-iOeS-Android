package com.pickflow.android.core.services.protocols

interface SpotService {
    suspend fun spot(id: String): SpotDetail
    suspend fun preview(id: String, coordinates: Coordinates? = null): SpotPreview
    suspend fun register(draft: SpotDraft): Spot
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
