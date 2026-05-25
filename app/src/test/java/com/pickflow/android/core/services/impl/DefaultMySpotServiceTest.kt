package com.pickflow.android.core.services.impl

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.pickflow.android.core.network.ApiException
import com.pickflow.android.core.network.api.MySpotApi
import com.pickflow.android.core.services.protocols.Coordinates
import com.pickflow.android.core.services.protocols.MySpotStatus
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

class DefaultMySpotServiceTest {

    private lateinit var server: MockWebServer
    private lateinit var service: DefaultMySpotService

    @BeforeEach
    fun setUp() {
        server = MockWebServer().apply { start() }
        val json = Json { ignoreUnknownKeys = true; explicitNulls = false }
        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        service = DefaultMySpotService(retrofit.create(MySpotApi::class.java))
    }

    @AfterEach
    fun tearDown() = server.shutdown()

    @Test
    fun `list with coords maps three status values + theme + distance`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {"success":true,"code":"OK","message":"","data":{
                  "spots":[
                    {"spotId":1,"name":"대기","theme":"SUNSET","imageUrl":"https://t/1.png","latitude":37.5,"longitude":127.0,"distanceKm":0.2,"createdAt":"2026-05-25T10:00:00Z","status":"PENDING","bookmarkCount":0},
                    {"spotId":2,"name":"공개","theme":"YUNSEUL","imageUrl":null,"latitude":37.6,"longitude":127.1,"distanceKm":1.5,"createdAt":"2026-05-20T10:00:00Z","status":"PUBLISHED","bookmarkCount":42},
                    {"spotId":3,"name":"반려","theme":"SUNSET","imageUrl":"  ","latitude":37.7,"longitude":127.2,"distanceKm":3.0,"createdAt":"2026-05-15T10:00:00Z","status":"REJECTED","bookmarkCount":0}
                  ],
                  "page":0,"hasNext":true
                }}
                """.trimIndent()
            )
        )

        val pg = service.list(page = 0, coordinates = Coordinates(37.5, 127.0))

        assertEquals(3, pg.items.size)
        assertEquals(MySpotStatus.PENDING, pg.items[0].status)
        assertEquals(MySpotStatus.PUBLISHED, pg.items[1].status)
        assertEquals(MySpotStatus.REJECTED, pg.items[2].status)
        assertEquals(SpotTheme.YUNSEUL, pg.items[1].theme)
        assertEquals(42L, pg.items[1].bookmarkCount)
        assertNull(pg.items[2].imageUrl) // blank → null

        val url = server.takeRequest().requestUrl!!
        assertEquals("/v1/users/me/my-spots", url.encodedPath)
        assertEquals("0", url.queryParameter("page"))
        assertEquals("37.5", url.queryParameter("latitude"))
        assertEquals("127.0", url.queryParameter("longitude"))
    }

    @Test
    fun `list without coords omits lat lng and unknown status falls back to PENDING`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {"success":true,"code":"OK","message":"","data":{
                  "spots":[
                    {"spotId":1,"name":"unk","theme":"???","imageUrl":null,"latitude":0,"longitude":0,"distanceKm":null,"createdAt":"","status":"???","bookmarkCount":0}
                  ],
                  "page":3,"hasNext":false
                }}
                """.trimIndent()
            )
        )

        val pg = service.list(page = 3)
        assertEquals(3, pg.page)
        assertEquals(false, pg.hasNext)
        assertEquals(MySpotStatus.PENDING, pg.items[0].status) // unknown → PENDING
        assertEquals(SpotTheme.SUNSET, pg.items[0].theme) // unknown → SUNSET

        val url = server.takeRequest().requestUrl!!
        assertNull(url.queryParameter("latitude"))
        assertNull(url.queryParameter("longitude"))
    }

    @Test
    fun `list propagates ApiException on success=false`() {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"success":false,"code":"MY_001","message":"unauthorized","data":null}"""
            )
        )
        val ex = assertThrows(ApiException::class.java) {
            runBlocking { service.list(page = 0) }
        }
        assertEquals("MY_001", ex.code)
    }
}
