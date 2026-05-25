package com.pickflow.android.core.services.impl

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.pickflow.android.core.network.ApiException
import com.pickflow.android.core.network.api.UserApi
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

class DefaultUserServiceTest {

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
    fun `fetchMyPage maps fields and normalizes blank profileImage to null`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {"success":true,"code":"OK","message":"","data":{
                  "profileImageUrl":"  ","nickname":"pickflower#1234",
                  "savedSpotCount":12,"recordedSpotCount":3
                }}
                """.trimIndent()
            )
        )

        val home = service.fetchMyPage()
        assertEquals("pickflower#1234", home.nickname)
        assertNull(home.profileImageUrl)
        assertEquals(12L, home.savedSpotCount)
        assertEquals(3L, home.recordedSpotCount)
        assertEquals("/v1/users/me", server.takeRequest().path)
    }

    @Test
    fun `fetchUserName default delegates to fetchMyPage nickname`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"success":true,"code":"OK","message":"","data":{
                  "profileImageUrl":"https://img/p.png","nickname":"Alice#0001",
                  "savedSpotCount":0,"recordedSpotCount":0
                }}"""
            )
        )

        assertEquals("Alice#0001", service.fetchUserName())
    }

    @Test
    fun `fetchMyPage propagates ApiException on success=false`() {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"success":false,"code":"USR_001","message":"not auth","data":null}"""
            )
        )
        val ex = assertThrows(ApiException::class.java) {
            runBlocking { service.fetchMyPage() }
        }
        assertEquals("USR_001", ex.code)
    }
}
