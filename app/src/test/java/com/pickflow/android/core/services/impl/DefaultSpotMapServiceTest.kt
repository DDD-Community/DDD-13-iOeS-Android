package com.pickflow.android.core.services.impl

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.pickflow.android.core.network.ApiException
import com.pickflow.android.core.network.api.SpotApi
import com.pickflow.android.core.services.protocols.Coordinates
import com.pickflow.android.core.services.protocols.SpotTheme
import com.pickflow.android.core.services.protocols.ViewportBox
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

class DefaultSpotMapServiceTest {

    private lateinit var server: MockWebServer
    private lateinit var service: DefaultSpotMapService

    private val box = ViewportBox(
        topLeft = Coordinates(37.6, 126.9),
        topRight = Coordinates(37.6, 127.1),
        bottomLeft = Coordinates(37.4, 126.9),
        bottomRight = Coordinates(37.4, 127.1),
    )

    @BeforeEach
    fun setUp() {
        server = MockWebServer().apply { start() }
        val json = Json { ignoreUnknownKeys = true; explicitNulls = false }
        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        service = DefaultSpotMapService(retrofit.create(SpotApi::class.java))
    }

    @AfterEach
    fun tearDown() = server.shutdown()

    @Test
    fun `fetchInViewport sends all 8 coordinate query params and theme`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"success":true,"code":"OK","message":"","data":{"spots":[]}}"""
            )
        )

        service.fetchInViewport(box, setOf(SpotTheme.SUNSET))

        val req = server.takeRequest()
        val url = req.requestUrl!!
        assertEquals("/v1/spots/viewport", url.encodedPath)
        assertEquals("37.6", url.queryParameter("topLeftLat"))
        assertEquals("126.9", url.queryParameter("topLeftLng"))
        assertEquals("37.6", url.queryParameter("topRightLat"))
        assertEquals("127.1", url.queryParameter("topRightLng"))
        assertEquals("37.4", url.queryParameter("bottomLeftLat"))
        assertEquals("126.9", url.queryParameter("bottomLeftLng"))
        assertEquals("37.4", url.queryParameter("bottomRightLat"))
        assertEquals("127.1", url.queryParameter("bottomRightLng"))
        assertEquals("SUNSET", url.queryParameter("theme"))
    }

    @Test
    fun `fetchInViewport with multiple themes sends one repeated theme param per selection`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"success":true,"code":"OK","message":"","data":{"spots":[]}}"""
            )
        )

        service.fetchInViewport(box, setOf(SpotTheme.NIGHT, SpotTheme.SUNLIGHT))

        val url = server.takeRequest().requestUrl!!
        assertEquals(listOf("SUNLIGHT", "NIGHT"), url.queryParameterValues("theme"))
    }

    @Test
    fun `fetchInViewport without theme omits theme query and maps markers`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {"success":true,"code":"OK","message":"","data":{"spots":[
                  {"spotId":1,"spotImageUrl":"https://img/1.png","latitude":37.5,"longitude":127.0,"isMySpot":false},
                  {"spotId":2,"spotImageUrl":null,"latitude":37.6,"longitude":127.1,"isMySpot":true},
                  {"spotId":3,"spotImageUrl":" ","latitude":37.7,"longitude":127.2,"isMySpot":false}
                ]}}
                """.trimIndent()
            )
        )

        val markers = service.fetchInViewport(box, themes = emptySet())

        val req = server.takeRequest()
        assertNull(req.requestUrl!!.queryParameter("theme"))
        assertEquals(3, markers.size)
        assertEquals(1L, markers[0].spotId)
        assertEquals("https://img/1.png", markers[0].imageUrl)
        assertEquals(true, markers[1].isMySpot)
        assertNull(markers[1].imageUrl)
        assertNull(markers[2].imageUrl) // blank → null
    }

    @Test
    fun `fetchInViewport propagates ApiException on success=false`() {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"success":false,"code":"VIEW_001","message":"invalid box","data":null}"""
            )
        )
        val ex = assertThrows(ApiException::class.java) {
            runBlocking { service.fetchInViewport(box) }
        }
        assertEquals("VIEW_001", ex.code)
    }
}
