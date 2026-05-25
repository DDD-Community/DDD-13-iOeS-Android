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

class DefaultBookmarkServiceTest {

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
    fun `add sends POST and returns bookmarkCount, also updates local cache`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(201).setBody(
                """{"success":true,"code":"OK","message":"","data":{"bookmarkCount":7}}"""
            )
        )

        val count = service.add("42")

        assertEquals(7L, count)
        assertTrue(service.isBookmarked("42"))
        assertEquals(setOf("42"), service.bookmarkedIds())

        val req = server.takeRequest()
        assertEquals("POST", req.method)
        assertEquals("/v1/spots/42/bookmarks", req.path)
    }

    @Test
    fun `add rejects non-numeric id and leaves cache untouched`() = runBlocking {
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { service.add("not-a-long") }
        }
        assertFalse(service.isBookmarked("not-a-long"))
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `add propagates ApiException on success=false and leaves cache untouched`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"success":false,"code":"BOOK_001","message":"already bookmarked","data":null}"""
            )
        )

        val ex = assertThrows(ApiException::class.java) {
            runBlocking { service.add("1") }
        }
        assertEquals("BOOK_001", ex.code)
        assertFalse(service.isBookmarked("1"))
    }

    @Test
    fun `legacy toggle still works as in-memory cache (will be replaced in remove iter)`() = runBlocking {
        assertFalse(service.isBookmarked("9"))
        assertTrue(service.toggle("9"))
        assertTrue(service.isBookmarked("9"))
        assertFalse(service.toggle("9"))
        assertFalse(service.isBookmarked("9"))
        assertEquals(0, server.requestCount)
    }
}
