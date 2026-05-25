package com.pickflow.android.core.services.impl

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.pickflow.android.core.network.ApiException
import com.pickflow.android.core.network.api.MySpotAlarmApi
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

class DefaultMySpotAlarmServiceGetTest {

    private lateinit var server: MockWebServer
    private lateinit var service: DefaultMySpotAlarmService

    @BeforeEach
    fun setUp() {
        server = MockWebServer().apply { start() }
        val json = Json { ignoreUnknownKeys = true; explicitNulls = false }
        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        service = DefaultMySpotAlarmService(retrofit.create(MySpotAlarmApi::class.java))
    }

    @AfterEach
    fun tearDown() = server.shutdown()

    @Test
    fun `get returns enabled true when subscribed`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"success":true,"code":"OK","message":"","data":{"spotId":42,"enabled":true}}"""
            )
        )

        val alarm = service.get(42L)

        assertEquals(42L, alarm.spotId)
        assertTrue(alarm.enabled)

        val req = server.takeRequest()
        assertEquals("GET", req.method)
        assertEquals("/v1/users/me/my-spots/42/alarm", req.path)
    }

    @Test
    fun `get returns enabled false when no subscription history`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"success":true,"code":"OK","message":"","data":{"spotId":7,"enabled":false}}"""
            )
        )

        val alarm = service.get(7L)
        assertFalse(alarm.enabled)
    }

    @Test
    fun `get propagates ApiException on failure`() {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"success":false,"code":"ALARM_403","message":"not your spot","data":null}"""
            )
        )
        val ex = assertThrows(ApiException::class.java) {
            runBlocking { service.get(99L) }
        }
        assertEquals("ALARM_403", ex.code)
    }
}
