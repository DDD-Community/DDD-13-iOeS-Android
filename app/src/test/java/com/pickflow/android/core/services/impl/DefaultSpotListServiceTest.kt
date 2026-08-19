package com.pickflow.android.core.services.impl

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.pickflow.android.core.network.ApiException
import com.pickflow.android.core.network.api.SpotApi
import com.pickflow.android.core.services.protocols.Coordinates
import com.pickflow.android.core.services.protocols.SpotSort
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

class DefaultSpotListServiceTest {

    private lateinit var server: MockWebServer
    private lateinit var service: DefaultSpotListService

    @BeforeEach
    fun setUp() {
        server = MockWebServer().apply { start() }
        val json = Json { ignoreUnknownKeys = true; explicitNulls = false }
        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        service = DefaultSpotListService(retrofit.create(SpotApi::class.java))
    }

    @AfterEach
    fun tearDown() = server.shutdown()

    @Test
    fun `fetch with DISTANCE sort sends coordinates and theme query`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {"success":true,"code":"OK","message":"","data":{
                  "spots":[{"spotId":1,"name":"노을 스팟","theme":"SUNSET","thumbnailUrl":"https://t/1.png","distanceKm":1.2}],
                  "page":0,"hasNext":true
                }}
                """.trimIndent()
            )
        )

        val page = service.fetch(
            themes = setOf(SpotTheme.SUNSET),
            page = 0,
            coordinates = Coordinates(37.5, 127.0),
            sort = SpotSort.DISTANCE,
        )

        assertEquals(1, page.items.size)
        assertEquals("1", page.items[0].id)
        assertEquals(SpotTheme.SUNSET, page.items[0].theme)
        assertEquals(1.2, page.items[0].distanceKm)
        assertEquals(true, page.hasNext)

        val req = server.takeRequest()
        val url = req.requestUrl!!
        assertEquals("/v1/spots", url.encodedPath)
        assertEquals("0", url.queryParameter("page"))
        assertEquals("SUNSET", url.queryParameter("theme"))
        assertEquals("37.5", url.queryParameter("latitude"))
        assertEquals("127.0", url.queryParameter("longitude"))
        assertEquals("DISTANCE", url.queryParameter("sort"))
    }

    @Test
    fun `fetch without coordinates omits lat lng and uses RECOMMENDED default`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"success":true,"code":"OK","message":"","data":{"spots":[],"page":3,"hasNext":false}}"""
            )
        )

        val page = service.fetch(themes = emptySet(), page = 3)
        assertEquals(3, page.page)
        assertEquals(false, page.hasNext)
        assertEquals(0, page.items.size)

        val url = server.takeRequest().requestUrl!!
        assertEquals("3", url.queryParameter("page"))
        assertNull(url.queryParameter("theme"))
        assertNull(url.queryParameter("latitude"))
        assertNull(url.queryParameter("longitude"))
        assertEquals("RECOMMENDED", url.queryParameter("sort"))
    }

    @Test
    fun `fetch propagates ApiException on success=false`() {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"success":false,"code":"LIST_001","message":"invalid sort","data":null}"""
            )
        )
        val ex = assertThrows(ApiException::class.java) {
            runBlocking { service.fetch(themes = emptySet(), page = 0) }
        }
        assertEquals("LIST_001", ex.code)
    }

    @Test
    fun `fetch with multiple themes sends one repeated theme param per selection`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"success":true,"code":"OK","message":"","data":{"spots":[],"page":0,"hasNext":false}}"""
            )
        )

        // 선택 순서와 무관하게 SpotTheme 선언 순서(햇살→윤슬→노을→야경)로 직렬화된다.
        service.fetch(themes = setOf(SpotTheme.NIGHT_VIEW, SpotTheme.SUNLIGHT), page = 0)

        val url = server.takeRequest().requestUrl!!
        assertEquals(listOf("SUNLIGHT", "NIGHT_VIEW"), url.queryParameterValues("theme"))
    }
}
