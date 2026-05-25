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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import retrofit2.Retrofit

class DefaultBoardServiceDetailTest {

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
    fun `detail sends postId path + masterId query and maps body`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {"success":true,"code":"OK","message":"","data":{
                  "masterId":1,"postId":42,"title":"[공지] 신규 업데이트",
                  "createdAt":"2026-05-25",
                  "content":"앱이 더 빨라졌어요.\n자세한 내용은 본문을 확인하세요."
                }}
                """.trimIndent()
            )
        )

        val detail = service.detail(masterId = 1L, postId = 42L)

        assertEquals(1L, detail.masterId)
        assertEquals(42L, detail.postId)
        assertEquals("[공지] 신규 업데이트", detail.title)
        assertEquals("2026-05-25", detail.createdAt)
        assertEquals("앱이 더 빨라졌어요.\n자세한 내용은 본문을 확인하세요.", detail.content)

        val req = server.takeRequest()
        assertEquals("GET", req.method)
        val url = req.requestUrl!!
        assertEquals("/v1/bbs/posts/42", url.encodedPath)
        assertEquals("1", url.queryParameter("masterId"))
    }

    @Test
    fun `detail preserves long content body verbatim`() = runBlocking {
        val long = "x".repeat(1000)
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"success":true,"code":"OK","message":"","data":{
                  "masterId":2,"postId":9,"title":"long","createdAt":"2026-05-20","content":"$long"
                }}"""
            )
        )

        val detail = service.detail(masterId = 2L, postId = 9L)
        assertEquals(1000, detail.content.length)
    }

    @Test
    fun `detail propagates ApiException on success=false`() {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"success":false,"code":"BBS_POST_404","message":"not found","data":null}"""
            )
        )
        val ex = assertThrows(ApiException::class.java) {
            runBlocking { service.detail(masterId = 1L, postId = 999L) }
        }
        assertEquals("BBS_POST_404", ex.code)
    }
}
