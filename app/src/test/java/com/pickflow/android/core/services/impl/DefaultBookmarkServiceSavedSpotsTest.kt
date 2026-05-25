package com.pickflow.android.core.services.impl

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.pickflow.android.core.network.ApiException
import com.pickflow.android.core.network.api.BookmarkApi
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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import retrofit2.Retrofit

class DefaultBookmarkServiceSavedSpotsTest {

    private lateinit var server: MockWebServer
    private lateinit var service: DefaultBookmarkService

    @BeforeEach
    fun setUp() {
        server = MockWebServer().apply { start() }
        val json = Json { ignoreUnknownKeys = true; explicitNulls = false }
        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        service = DefaultBookmarkService(retrofit.create(BookmarkApi::class.java))
    }

    @AfterEach
    fun tearDown() = server.shutdown()

    @Test
    fun `savedSpots with coordinates sends page+lat+lng and maps items`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {"success":true,"code":"OK","message":"","data":{
                  "spots":[
                    {"spotId":1,"name":"노을 한강","theme":"SUNSET","imageUrl":"https://t/1.png","latitude":37.5,"longitude":127.0,"distanceKm":0.3,"savedAt":"2026-05-25T10:00:00Z","deleted":false},
                    {"spotId":2,"name":"윤슬","theme":"YUNSEUL","imageUrl":null,"latitude":37.6,"longitude":127.1,"distanceKm":null,"savedAt":"2026-05-20T10:00:00Z","deleted":true}
                  ],
                  "page":0,"hasNext":true
                }}
                """.trimIndent()
            )
        )

        val pg = service.savedSpots(page = 0, coordinates = Coordinates(37.5, 127.0))

        assertEquals(0, pg.page)
        assertTrue(pg.hasNext)
        assertEquals(2, pg.items.size)
        assertEquals(1L, pg.items[0].id)
        assertEquals(SpotTheme.SUNSET, pg.items[0].theme)
        assertEquals(0.3, pg.items[0].distanceKm)
        assertEquals(SpotTheme.YUNSEUL, pg.items[1].theme)
        assertEquals(true, pg.items[1].deleted)
        assertNull(pg.items[1].imageUrl)

        val url = server.takeRequest().requestUrl!!
        assertEquals("/v1/users/me/saved-spots", url.encodedPath)
        assertEquals("0", url.queryParameter("page"))
        assertEquals("37.5", url.queryParameter("latitude"))
        assertEquals("127.0", url.queryParameter("longitude"))
    }

    @Test
    fun `savedSpots without coordinates omits lat lng and yields null distance`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"success":true,"code":"OK","message":"","data":{
                  "spots":[],"page":2,"hasNext":false
                }}"""
            )
        )

        val pg = service.savedSpots(page = 2)
        assertEquals(2, pg.page)
        assertEquals(0, pg.items.size)

        val url = server.takeRequest().requestUrl!!
        assertEquals("2", url.queryParameter("page"))
        assertNull(url.queryParameter("latitude"))
        assertNull(url.queryParameter("longitude"))
    }

    @Test
    fun `savedSpots propagates ApiException on failure`() {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"success":false,"code":"SAV_001","message":"unauthorized","data":null}"""
            )
        )
        val ex = assertThrows(ApiException::class.java) {
            runBlocking { service.savedSpots(page = 0) }
        }
        assertEquals("SAV_001", ex.code)
    }
}
