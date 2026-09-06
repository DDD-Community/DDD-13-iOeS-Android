package com.pickflow.android.core.services.impl

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.pickflow.android.core.network.api.LikeApi
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import retrofit2.Retrofit

class DefaultLikeServiceTest {

    private lateinit var server: MockWebServer
    private lateinit var service: DefaultLikeService

    @BeforeEach
    fun setUp() {
        server = MockWebServer().apply { start() }
        val json = Json { ignoreUnknownKeys = true; explicitNulls = false }
        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        service = DefaultLikeService(retrofit.create(LikeApi::class.java))
    }

    @AfterEach
    fun tearDown() = server.shutdown()

    private fun enqueue(code: Int, body: String) {
        server.enqueue(
            MockResponse().setResponseCode(code).setBody(
                """{"success":true,"code":"S000","message":"ok","data":$body}""",
            ),
        )
    }

    @Test
    fun `add 는 POST likes 를 치고 갱신된 likeCount 를 돌려준다`() = runBlocking {
        enqueue(201, """{"spotId":42,"likeCount":8}""")

        val count = service.add("42")

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/v1/spots/42/likes", request.path)
        assertEquals(8L, count)
    }

    @Test
    fun `remove 는 DELETE likes 를 치고 갱신된 likeCount 를 돌려준다`() = runBlocking {
        enqueue(200, """{"spotId":42,"likeCount":7}""")

        val count = service.remove("42")

        val request = server.takeRequest()
        assertEquals("DELETE", request.method)
        assertEquals("/v1/spots/42/likes", request.path)
        assertEquals(7L, count)
    }

    @Test
    fun `정수가 아닌 spotId 는 호출 전에 거른다`() {
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { service.add("spot-42") }
        }
        assertEquals(0, server.requestCount)
    }
}
