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

/**
 * iOS `UserService.updateProfile` 1:1 — multipart PATCH 계약 검증.
 * nickname 은 텍스트 part, profileImage 는 파일 part 로 전송된다.
 */
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
    fun `updateProfile with nickname and image sends both multipart parts`() = runBlocking {
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
        assertEquals("/v1/users/me", req.requestUrl!!.encodedPath)
        val contentType = req.getHeader("Content-Type") ?: ""
        assertTrue(contentType.startsWith("multipart/form-data"))
        val body = req.body.readUtf8()
        // nickname 텍스트 part
        assertTrue(body.contains("name=\"nickname\""))
        assertTrue(body.contains("Alice"))
        // profileImage 파일 part
        assertTrue(body.contains("name=\"profileImage\""))
        assertTrue(body.contains("filename=\"avatar.png\""))
        assertTrue(body.contains("Content-Type: image/png"))
    }

    @Test
    fun `updateProfile with only nickname sends nickname part without image part`() = runBlocking {
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
        val contentType = req.getHeader("Content-Type") ?: ""
        assertTrue(contentType.startsWith("multipart/form-data"))
        val body = req.body.readUtf8()
        assertTrue(body.contains("name=\"nickname\""))
        assertTrue(body.contains("Bob"))
        assertTrue(!body.contains("profileImage"))
    }

    @Test
    fun `updateProfile with neither field fails fast without network call`() {
        val ex = assertThrows(IllegalArgumentException::class.java) {
            runBlocking { service.updateProfile() }
        }
        assertEquals("변경된 항목이 없습니다.", ex.message)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `updateProfile succeeds even when data is null (iOS EmptyResponse parity)`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"success":true,"code":"OK","message":"","data":null}"""
            )
        )

        val result = service.updateProfile(nickname = "Alice")

        assertEquals("Alice", result.displayName)
        assertNull(result.profileImageUrl)
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
