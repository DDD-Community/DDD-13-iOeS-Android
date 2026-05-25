package com.pickflow.android.core.services.impl

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.pickflow.android.core.network.ApiException
import com.pickflow.android.core.network.api.UserApi
import com.pickflow.android.core.services.protocols.ImagePayload
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import retrofit2.Retrofit

class DefaultUserServiceUpdateProfileTest {

    private lateinit var server: MockWebServer
    private lateinit var service: DefaultUserService

    @BeforeEach
    fun setUp() {
        server = MockWebServer().apply { start() }
        val json = Json { ignoreUnknownKeys = true; explicitNulls = false }
        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        service = DefaultUserService(retrofit.create(UserApi::class.java))
    }

    @AfterEach
    fun tearDown() = server.shutdown()

    @Test
    fun `updateProfile with nickname and image sends query and multipart part`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"success":true,"code":"OK","message":"","data":{
                  "displayName":"Alice#0001","profileImageUrl":"https://img/p.png"
                }}"""
            )
        )

        val payload = ImagePayload(
            bytes = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47),
            mimeType = "image/png",
            filename = "avatar.png",
        )
        val result = service.updateProfile(nickname = "Alice", profileImage = payload)

        assertEquals("Alice#0001", result.displayName)
        assertEquals("https://img/p.png", result.profileImageUrl)

        val req = server.takeRequest()
        assertEquals("PATCH", req.method)
        val url = req.requestUrl!!
        assertEquals("/v1/users/me", url.encodedPath)
        assertEquals("Alice", url.queryParameter("nickname"))
        val contentType = req.getHeader("Content-Type") ?: ""
        assertTrue(contentType.startsWith("multipart/form-data"))
        val body = req.body.readUtf8()
        assertTrue(body.contains("name=\"profileImage\""))
        assertTrue(body.contains("filename=\"avatar.png\""))
        assertTrue(body.contains("Content-Type: image/png"))
    }

    @Test
    fun `updateProfile with only nickname omits multipart part body`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"success":true,"code":"OK","message":"","data":{
                  "displayName":"Bob#0002","profileImageUrl":""
                }}"""
            )
        )

        val result = service.updateProfile(nickname = "Bob", profileImage = null)

        assertEquals("Bob#0002", result.displayName)
        assertNull(result.profileImageUrl) // blank → null

        val req = server.takeRequest()
        assertEquals("Bob", req.requestUrl!!.queryParameter("nickname"))
        val body = req.body.readUtf8()
        assertTrue(!body.contains("profileImage"))
    }

    @Test
    fun `updateProfile with neither field still PATCHes and parses`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"success":true,"code":"OK","message":"","data":{
                  "displayName":"unchanged#0003","profileImageUrl":null
                }}"""
            )
        )

        val result = service.updateProfile()
        assertEquals("unchanged#0003", result.displayName)
        assertNull(result.profileImageUrl)

        val url = server.takeRequest().requestUrl!!
        assertNull(url.queryParameter("nickname"))
    }

    @Test
    fun `updateProfile propagates ApiException on success=false`() {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"success":false,"code":"USR_VALID","message":"닉네임 중복","data":null}"""
            )
        )
        val ex = assertThrows(ApiException::class.java) {
            runBlocking { service.updateProfile(nickname = "dup") }
        }
        assertEquals("USR_VALID", ex.code)
    }
}
