package com.pickflow.android.core.services.impl

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.pickflow.android.core.network.ApiException
import com.pickflow.android.core.network.api.ArchiveApi
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

class DefaultArchiveServiceUpdateNameTest {

    private lateinit var server: MockWebServer
    private lateinit var service: DefaultArchiveService

    @BeforeEach
    fun setUp() {
        server = MockWebServer().apply { start() }
        val json = Json { ignoreUnknownKeys = true; explicitNulls = false }
        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        service = DefaultArchiveService(retrofit.create(ArchiveApi::class.java))
    }

    @AfterEach
    fun tearDown() = server.shutdown()

    @Test
    fun `updateName sends PATCH with archiveName body`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"success":true,"code":"OK","message":"","data":{
                  "archiveName":"여행 보관함","archiveImageUrl":"https://s3/p.png"
                }}"""
            )
        )

        val updated = service.updateName("여행 보관함")

        assertEquals("여행 보관함", updated.name)
        assertEquals("https://s3/p.png", updated.imageUrl)

        val req = server.takeRequest()
        assertEquals("PATCH", req.method)
        assertEquals("/v1/users/me/archive/name", req.path)
        assertEquals("""{"archiveName":"여행 보관함"}""", req.body.readUtf8())
    }

    @Test
    fun `updateName with max 20 chars succeeds`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"success":true,"code":"OK","message":"","data":{
                  "archiveName":"a".repeat(20),"archiveImageUrl":null
                }}""".trimIndent().replace("\"a\".repeat(20)", "\"" + "a".repeat(20) + "\"")
            )
        )

        val updated = service.updateName("a".repeat(20))
        assertEquals("a".repeat(20), updated.name)
        assertEquals(null, updated.imageUrl)
    }

    @Test
    fun `updateName propagates ApiException on validation failure`() {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"success":false,"code":"ARC_NAME","message":"too long","data":null}"""
            )
        )
        val ex = assertThrows(ApiException::class.java) {
            runBlocking { service.updateName("a".repeat(30)) }
        }
        assertEquals("ARC_NAME", ex.code)
    }
}
