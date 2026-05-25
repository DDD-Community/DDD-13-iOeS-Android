package com.pickflow.android.core.services.impl

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.pickflow.android.core.network.ApiException
import com.pickflow.android.core.network.api.SpotApi
import com.pickflow.android.core.services.protocols.CongestionLevel
import com.pickflow.android.core.services.protocols.Precipitation
import com.pickflow.android.core.services.protocols.SpotTheme
import com.pickflow.android.core.services.protocols.WeatherSky
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import retrofit2.Retrofit

class DefaultSpotServiceTest {

    private lateinit var server: MockWebServer
    private lateinit var service: DefaultSpotService

    @BeforeEach
    fun setUp() {
        server = MockWebServer().apply { start() }
        val json = Json { ignoreUnknownKeys = true; explicitNulls = false }
        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        service = DefaultSpotService(retrofit.create(SpotApi::class.java))
    }

    @AfterEach
    fun tearDown() = server.shutdown()

    @Test
    fun `spot() with full payload maps weather, congestion, nested fields`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {"success":true,"code":"OK","message":"","data":{
                  "spotId":42,
                  "name":"한강 노을 스팟","comment":"멋진 노을","theme":"SUNSET",
                  "latitude":37.5,"longitude":127.0,
                  "address":"서울 동작구","addressRoad":"동작대로 1","addressJibun":"본동 123-4",
                  "imageUrl":"https://img/42.png",
                  "recordedDate":"2026-05-25","recordedTime":"18:40",
                  "weatherSky":"MOSTLY_CLOUDY","precipitation":"NONE","precipitationProbability":15,
                  "congestionLevel":"NORMAL",
                  "sunsetTime":"18:45","astronomyDate":"2026-05-25",
                  "weatherUpdatedAt":"2026-05-25T17:00:00Z",
                  "congestionUpdatedAt":"2026-05-25T17:00:00Z",
                  "parkingInfo":"무료 주차장",
                  "bookmarkCount":7,"isBookmarked":true,"isMySpot":false
                }}
                """.trimIndent()
            )
        )

        val detail = service.spot("42")

        assertEquals(42L, detail.id)
        assertEquals(SpotTheme.SUNSET, detail.theme)
        assertEquals("동작대로 1", detail.addressRoad)
        assertNotNull(detail.weather)
        assertEquals(WeatherSky.MOSTLY_CLOUDY, detail.weather!!.sky)
        assertEquals(Precipitation.NONE, detail.weather!!.precipitation)
        assertEquals(15, detail.weather!!.precipitationProbability)
        assertNotNull(detail.congestion)
        assertEquals(CongestionLevel.NORMAL, detail.congestion!!.level)
        assertEquals(7L, detail.bookmarkCount)
        assertEquals(true, detail.isBookmarked)

        assertEquals("/v1/spots/42", server.takeRequest().path)
    }

    @Test
    fun `spot() with missing weather and congestion yields null sections`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {"success":true,"code":"OK","message":"","data":{
                  "spotId":1,"name":"n","comment":"","theme":"YUNSEUL",
                  "latitude":0,"longitude":0,"address":"",
                  "imageUrl":null,"recordedDate":"","recordedTime":"",
                  "weatherSky":null,"congestionLevel":null,
                  "bookmarkCount":0,"isBookmarked":false,"isMySpot":false
                }}
                """.trimIndent()
            )
        )

        val detail = service.spot("1")
        assertNull(detail.weather)
        assertNull(detail.congestion)
        assertNull(detail.imageUrl)
        assertEquals(SpotTheme.YUNSEUL, detail.theme)
    }

    @Test
    fun `spot() rejects non-numeric id with IllegalArgumentException`() {
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { service.spot("not-a-long") }
        }
    }

    @Test
    fun `spot() propagates ApiException on success=false`() {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"success":false,"code":"SPOT_404","message":"not found","data":null}"""
            )
        )
        val ex = assertThrows(ApiException::class.java) {
            runBlocking { service.spot("1") }
        }
        assertEquals("SPOT_404", ex.code)
    }
}
