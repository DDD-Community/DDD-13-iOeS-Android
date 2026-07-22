package com.pickflow.android.core.services.impl

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.pickflow.android.core.network.ApiException
import com.pickflow.android.core.network.api.MySpotApi
import com.pickflow.android.core.services.protocols.ImagePayload
import com.pickflow.android.core.services.protocols.MySpotStatus
import com.pickflow.android.core.services.protocols.SpotDraft
import com.pickflow.android.core.services.protocols.SpotTheme
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import retrofit2.Retrofit

class DefaultMySpotServiceCreateTest {

    private lateinit var server: MockWebServer
    private lateinit var service: DefaultMySpotService
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    @BeforeEach
    fun setUp() {
        server = MockWebServer().apply { start() }
        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        service = DefaultMySpotService(retrofit.create(MySpotApi::class.java), json)
    }

    @AfterEach
    fun tearDown() = server.shutdown()

    private fun draft() = SpotDraft(
        name = "한강 노을",
        theme = SpotTheme.SUNSET,
        latitude = 37.5,
        longitude = 127.0,
        address = "서울 동작구 한강대로",
        capturedDate = "2026-05-25",
        capturedTime = "18:30",
        comment = "강 위로 번지는 노을이 좋아요",
    )

    private fun image() = ImagePayload(
        bytes = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47),
        mimeType = "image/png",
        filename = "spot.png",
    )

    @Test
    fun `create posts multipart with image part and meta JSON part`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(201).setBody(
                """{"success":true,"code":"OK","message":"","data":{
                  "spotId":42,"status":"PENDING","imageUrl":"https://s3/42.png"
                }}"""
            )
        )

        val result = service.create(draft(), image())

        assertEquals(42L, result.spotId)
        assertEquals(MySpotStatus.PENDING, result.status)
        assertEquals("https://s3/42.png", result.imageUrl)

        val req = server.takeRequest()
        assertEquals("POST", req.method)
        assertEquals("/v1/users/me/my-spots", req.path)
        val contentType = req.getHeader("Content-Type") ?: ""
        assertTrue(contentType.startsWith("multipart/form-data"))
        val body = req.body.readUtf8()
        // image part
        assertTrue(body.contains("name=\"image\""))
        assertTrue(body.contains("filename=\"spot.png\""))
        assertTrue(body.contains("Content-Type: image/png"))
        // 메타 part — iOS `SpotService.registerSpot` 1:1: part 이름 `request`, address 미전송
        assertTrue(body.contains("name=\"request\""))
        assertTrue(body.contains("Content-Type: application/json"))
        assertTrue(body.contains("\"name\":\"한강 노을\""))
        assertTrue(body.contains("\"theme\":\"SUNSET\""))
        assertTrue(body.contains("\"latitude\":37.5"))
        assertTrue(body.contains("\"comment\":\"강 위로 번지는 노을이 좋아요\""))
        assertTrue(body.contains("\"recordedDate\":\"2026-05-25\""))
        assertTrue(!body.contains("\"address\""))
    }

    @Test
    fun `create with blank optional fields omits them from meta JSON`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(201).setBody(
                """{"success":true,"code":"OK","message":"","data":{
                  "spotId":1,"status":"PENDING","imageUrl":""
                }}"""
            )
        )

        val sparse = draft().copy(comment = "", capturedDate = "", capturedTime = "")
        val result = service.create(sparse, image())

        assertEquals(null, result.imageUrl) // blank → null
        val body = server.takeRequest().body.readUtf8()
        // explicitNulls=false + takeIf {isNotBlank} 로 빈 값은 직렬화에서 제외
        assertTrue(!body.contains("\"comment\""))
        assertTrue(!body.contains("\"recordedDate\""))
        assertTrue(!body.contains("\"recordedTime\""))
    }

    @Test
    fun `create propagates ApiException on success=false`() {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"success":false,"code":"MY_413","message":"image too large","data":null}"""
            )
        )
        val ex = assertThrows(ApiException::class.java) {
            runBlocking { service.create(draft(), image()) }
        }
        assertEquals("MY_413", ex.code)
    }
}
