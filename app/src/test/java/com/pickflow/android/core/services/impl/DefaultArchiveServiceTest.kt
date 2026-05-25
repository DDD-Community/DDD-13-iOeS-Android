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
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import retrofit2.Retrofit

class DefaultArchiveServiceTest {

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
    fun `fetch maps name and presigned image url`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"success":true,"code":"OK","message":"","data":{
                  "archiveName":"내 보관함","archiveImageUrl":"https://s3/presigned.png"
                }}"""
            )
        )

        val archive = service.fetch()
        assertEquals("내 보관함", archive.name)
        assertEquals("https://s3/presigned.png", archive.imageUrl)
        assertEquals("/v1/users/me/archive", server.takeRequest().path)
    }

    @Test
    fun `fetch normalizes blank imageUrl to null`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"success":true,"code":"OK","message":"","data":{
                  "archiveName":"빈 보관함","archiveImageUrl":"  "
                }}"""
            )
        )

        val archive = service.fetch()
        assertEquals("빈 보관함", archive.name)
        assertNull(archive.imageUrl)
    }

    @Test
    fun `fetch propagates ApiException on success=false`() {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"success":false,"code":"ARC_001","message":"forbidden","data":null}"""
            )
        )
        val ex = assertThrows(ApiException::class.java) {
            runBlocking { service.fetch() }
        }
        assertEquals("ARC_001", ex.code)
    }
}
