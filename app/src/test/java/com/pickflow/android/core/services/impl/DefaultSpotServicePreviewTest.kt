package com.pickflow.android.core.services.impl

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.pickflow.android.core.network.ApiException
import com.pickflow.android.core.network.api.SpotApi
import com.pickflow.android.core.services.protocols.Coordinates
import com.pickflow.android.core.services.protocols.SpotTheme
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import retrofit2.Retrofit

class DefaultSpotServicePreviewTest {

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
    fun `preview with coordinates sends lat lng and maps full payload`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {"success":true,"code":"OK","message":"","data":{
                  "spotId":42,"name":"한강 스팟","isMySpot":true,"theme":"YUNSEUL",
                  "bookmarkCount":12,"distanceKm":0.8,
                  "imageUrl":"https://img/42.png","addressSimple":"서울 동작구",
                  "addressRoad":"동작대로 1","addressJibun":"본동 123-4"
                }}
                """.trimIndent()
            )
        )

        val preview = service.preview("42", Coordinates(37.5, 127.0))

        assertEquals(42L, preview.id)
        assertEquals(SpotTheme.YUNSEUL, preview.theme)
        assertEquals(true, preview.isMySpot)
        assertEquals(0.8, preview.distanceKm)
        assertEquals("https://img/42.png", preview.imageUrl)
        assertEquals("동작대로 1", preview.addressRoad)

        val url = server.takeRequest().requestUrl!!
        assertEquals("/v1/spots/42/preview", url.encodedPath)
        assertEquals("37.5", url.queryParameter("latitude"))
        assertEquals("127.0", url.queryParameter("longitude"))
    }

    @Test
    fun `preview without coordinates omits lat lng and yields null distance`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {"success":true,"code":"OK","message":"","data":{
                  "spotId":1,"name":"n","isMySpot":false,"theme":"SUNSET",
                  "bookmarkCount":0,"distanceKm":null,
                  "imageUrl":" ","addressSimple":"서울",
                  "addressRoad":null,"addressJibun":""
                }}
                """.trimIndent()
            )
        )

        val preview = service.preview("1")
        assertNull(preview.distanceKm)
        assertNull(preview.imageUrl) // blank → null
        assertNull(preview.addressRoad)
        assertNull(preview.addressJibun) // blank → null

        val url = server.takeRequest().requestUrl!!
        assertNull(url.queryParameter("latitude"))
        assertNull(url.queryParameter("longitude"))
    }

    @Test
    fun `preview rejects non-numeric id`() {
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { service.preview("not-a-long") }
        }
    }

    @Test
    fun `preview propagates ApiException on success=false`() {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"success":false,"code":"PREV_404","message":"not found","data":null}"""
            )
        )
        val ex = assertThrows(ApiException::class.java) {
            runBlocking { service.preview("1") }
        }
        assertEquals("PREV_404", ex.code)
    }
}
