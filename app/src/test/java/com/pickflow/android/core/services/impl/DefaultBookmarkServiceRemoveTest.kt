package com.pickflow.android.core.services.impl

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.pickflow.android.core.network.ApiException
import com.pickflow.android.core.network.api.BookmarkApi
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import retrofit2.Retrofit

class DefaultBookmarkServiceRemoveTest {

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
    fun `remove sends DELETE and returns updated count, also removes from cache`() = runBlocking {
        // 먼저 add → cache 채움
        server.enqueue(
            MockResponse().setResponseCode(201).setBody(
                """{"success":true,"code":"OK","message":"","data":{"bookmarkCount":3}}"""
            )
        )
        service.add("7")
        server.takeRequest() // consume add

        // remove
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"success":true,"code":"OK","message":"","data":{"bookmarkCount":2}}"""
            )
        )

        val count = service.remove("7")

        assertEquals(2L, count)
        assertFalse(service.isBookmarked("7"))

        val req = server.takeRequest()
        assertEquals("DELETE", req.method)
        assertEquals("/v1/spots/7/bookmarks", req.path)
    }

    @Test
    fun `toggle routes to add when cache miss and to remove when cache hit`() = runBlocking {
        // 첫 toggle (cache miss) → add 호출
        server.enqueue(
            MockResponse().setResponseCode(201).setBody(
                """{"success":true,"code":"OK","message":"","data":{"bookmarkCount":1}}"""
            )
        )
        val t1 = service.toggle("5")
        assertTrue(t1)
        assertTrue(service.isBookmarked("5"))
        assertEquals("POST", server.takeRequest().method)

        // 두 번째 toggle (cache hit) → remove 호출
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"success":true,"code":"OK","message":"","data":{"bookmarkCount":0}}"""
            )
        )
        val t2 = service.toggle("5")
        assertFalse(t2)
        assertFalse(service.isBookmarked("5"))
        assertEquals("DELETE", server.takeRequest().method)
    }

    @Test
    fun `remove propagates ApiException and leaves cache as-is`() = runBlocking {
        // pre-fill cache with add
        server.enqueue(
            MockResponse().setResponseCode(201).setBody(
                """{"success":true,"code":"OK","message":"","data":{"bookmarkCount":1}}"""
            )
        )
        service.add("9")
        server.takeRequest()

        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"success":false,"code":"BOOK_404","message":"not found","data":null}"""
            )
        )
        val ex = assertThrows(ApiException::class.java) {
            runBlocking { service.remove("9") }
        }
        assertEquals("BOOK_404", ex.code)
        // 실패 시 캐시는 미변경 (서버에 여전히 있다고 가정)
        assertTrue(service.isBookmarked("9"))
    }
}
