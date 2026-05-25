package com.pickflow.android.core.services.impl

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.pickflow.android.core.network.ApiException
import com.pickflow.android.core.network.api.ArchiveApi
import com.pickflow.android.core.services.protocols.ImagePayload
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

class DefaultArchiveServiceUpdateImageTest {

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
    fun `updateImage uploads as multipart with archiveImage part`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"success":true,"code":"OK","message":"","data":{
                  "archiveName":"내 보관함","archiveImageUrl":"https://s3/new.png"
                }}"""
            )
        )

        val payload = ImagePayload(
            bytes = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47),
            mimeType = "image/png",
            filename = "archive.png",
        )
        val updated = service.updateImage(payload)

        assertEquals("내 보관함", updated.name)
        assertEquals("https://s3/new.png", updated.imageUrl)

        val req = server.takeRequest()
        assertEquals("POST", req.method)
        assertEquals("/v1/users/me/archive", req.path)
        val contentType = req.getHeader("Content-Type") ?: ""
        assertTrue(contentType.startsWith("multipart/form-data"))
        val body = req.body.readUtf8()
        assertTrue(body.contains("name=\"archiveImage\""))
        assertTrue(body.contains("filename=\"archive.png\""))
        assertTrue(body.contains("Content-Type: image/png"))
    }

    @Test
    fun `updateImage with jpeg mime sends correct content type`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"success":true,"code":"OK","message":"","data":{
                  "archiveName":"jpg 보관함","archiveImageUrl":""
                }}"""
            )
        )

        val payload = ImagePayload(
            bytes = ByteArray(8),
            mimeType = "image/jpeg",
            filename = "p.jpg",
        )
        val updated = service.updateImage(payload)
        assertEquals(null, updated.imageUrl) // blank → null

        val body = server.takeRequest().body.readUtf8()
        assertTrue(body.contains("Content-Type: image/jpeg"))
        assertTrue(body.contains("filename=\"p.jpg\""))
    }

    @Test
    fun `updateImage propagates ApiException on success=false`() {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"success":false,"code":"ARC_413","message":"too large","data":null}"""
            )
        )
        val payload = ImagePayload(bytes = ByteArray(4), mimeType = "image/png", filename = "x.png")
        val ex = assertThrows(ApiException::class.java) {
            runBlocking { service.updateImage(payload) }
        }
        assertEquals("ARC_413", ex.code)
    }
}
