package com.pickflow.android.core.services.protocols

data class AddressSuggestion(
    val displayName: String,
    val latitude: Double,
    val longitude: Double,
)

interface AddressService {
    suspend fun search(query: String): List<AddressSuggestion>
}
