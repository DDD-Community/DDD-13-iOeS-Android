package com.pickflow.android.core.services.impl

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.pickflow.android.core.network.ApiException
import com.pickflow.android.core.network.api.BoardApi
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

class DefaultBoardServiceTest {

    private lateinit var server: MockWebServer
    private lateinit var service: DefaultBoardService

    @BeforeEach
    fun setUp() {
        server = MockWebServer().apply { start() }
        val json = Json { ignoreUnknownKeys = true; explicitNulls = false }
        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        service = DefaultBoardService(retrofit.create(BoardApi::class.java))
    }

    @AfterEach
    fun tearDown() = server.shutdown()

    @Test
    fun `posts sends masterId+page and preserves pinned order from server`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {"success":true,"code":"OK","message":"","data":{
                  "items":[
                    {"postId":99,"title":"[공지] 점검","createdAt":"2026-05-25","pinned":true},
                    {"postId":42,"title":"이번 주 추천 스팟","createdAt":"2026-05-24","pinned":false}
                  ],
                  "page":0,"hasNext":true
                }}
                """.trimIndent()
            )
        )

        val pg = service.posts(masterId = 1L, page = 0)

        assertEquals(0, pg.page)
        assertTrue(pg.hasNext)
        assertEquals(2, pg.items.size)
        assertEquals(true, pg.items[0].pinned)
        assertEquals(99L, pg.items[0].postId)
        assertEquals(false, pg.items[1].pinned)

        val url = server.takeRequest().requestUrl!!
        assertEquals("/v1/bbs/posts", url.encodedPath)
        assertEquals("1", url.queryParameter("masterId"))
        assertEquals("0", url.queryParameter("page"))
    }

    @Test
    fun `posts with hasNext false on later page emits empty items if any`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"success":true,"code":"OK","message":"","data":{
                  "items":[],"page":5,"hasNext":false
                }}"""
            )
        )

        val pg = service.posts(masterId = 7L, page = 5)
        assertEquals(5, pg.page)
        assertEquals(false, pg.hasNext)
        assertEquals(0, pg.items.size)

        val url = server.takeRequest().requestUrl!!
        assertEquals("7", url.queryParameter("masterId"))
        assertEquals("5", url.queryParameter("page"))
    }

    @Test
    fun `posts propagates ApiException on success=false`() {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"success":false,"code":"BBS_404","message":"master not found","data":null}"""
            )
        )
        val ex = assertThrows(ApiException::class.java) {
            runBlocking { service.posts(masterId = 999L) }
        }
        assertEquals("BBS_404", ex.code)
    }
}
