package com.pickflow.android.core.services.impl

import com.pickflow.android.core.services.protocols.AddressService
import com.pickflow.android.core.services.protocols.AddressSuggestion
import javax.inject.Inject

class StubAddressService @Inject constructor() : AddressService {
    override suspend fun search(query: String): List<AddressSuggestion> {
        if (query.isBlank()) return emptyList()
        return List(3) { idx ->
            AddressSuggestion(
                displayName = "$query 결과 #${idx + 1}",
                latitude = 37.5 + idx * 0.01,
                longitude = 127.0 + idx * 0.01,
            )
        }
    }
}
