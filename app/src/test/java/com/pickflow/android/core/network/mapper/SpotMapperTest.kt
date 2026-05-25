package com.pickflow.android.core.network.mapper

import com.pickflow.android.core.network.dto.spot.SpotSummaryDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class SpotMapperTest {

    @Test
    fun `toMapMarker maps fields and coerces blank imageUrl to null`() {
        val dto = SpotSummaryDto(spotId = 42, spotImageUrl = " ", latitude = 1.0, longitude = 2.0, isMySpot = true)
        val marker = dto.toMapMarker()
        assertEquals(42L, marker.spotId)
        assertNull(marker.imageUrl)
        assertEquals(1.0, marker.coordinates.latitude)
        assertEquals(2.0, marker.coordinates.longitude)
        assertEquals(true, marker.isMySpot)
    }
}
