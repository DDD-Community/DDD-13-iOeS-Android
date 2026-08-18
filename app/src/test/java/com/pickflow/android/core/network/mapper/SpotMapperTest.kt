package com.pickflow.android.core.network.mapper

import com.pickflow.android.core.network.dto.spot.SpotItemDto
import com.pickflow.android.core.network.dto.spot.SpotSummaryDto
import com.pickflow.android.core.services.protocols.SpotTheme
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SpotMapperTest {

    private val json = Json { ignoreUnknownKeys = true }

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

    @Test
    fun `SpotItemDto decodes bookmark and like fields from the server payload`() {
        // GET /v1/spots 실제 응답 1건. 필드가 없으면 ignoreUnknownKeys 로 조용히 버려진다.
        val payload = """
            {
              "spotId": 3,
              "name": "서울숲 억새길",
              "theme": "SS",
              "thumbnailUrl": "https://cdn.example/3.jpg",
              "distanceKm": 4.52,
              "bookmarkCount": 5,
              "isBookmarked": true,
              "likeCount": 34,
              "isLiked": true
            }
        """.trimIndent()

        val dto = json.decodeFromString<SpotItemDto>(payload)

        assertEquals(5L, dto.bookmarkCount)
        assertTrue(dto.isBookmarked)
        assertEquals(34L, dto.likeCount)
        assertTrue(dto.isLiked)
    }

    @Test
    fun `toSpot carries bookmark and like fields`() {
        val spot = SpotItemDto(
            spotId = 3,
            name = "서울숲 억새길",
            theme = "SS",
            thumbnailUrl = "https://cdn.example/3.jpg",
            distanceKm = 4.52,
            bookmarkCount = 5,
            isBookmarked = true,
            likeCount = 34,
            isLiked = false,
        ).toSpot()

        assertEquals("3", spot.id)
        assertEquals(SpotTheme.SUNSET, spot.theme)
        assertEquals(5L, spot.bookmarkCount)
        assertTrue(spot.isBookmarked)
        assertEquals(34L, spot.likeCount)
        assertFalse(spot.isLiked)
    }
}
