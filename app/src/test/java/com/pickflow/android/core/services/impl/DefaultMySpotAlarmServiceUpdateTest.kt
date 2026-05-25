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

class DefaultMySpotAlarmServiceUpdateTest {

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
    fun `update enabled true sends PUT with body and returns server response`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"success":true,"code":"OK","message":"","data":{"spotId":42,"enabled":true}}"""
            )
        )

        val alarm = service.update(spotId = 42L, enabled = true)

        assertEquals(42L, alarm.spotId)
        assertTrue(alarm.enabled)

        val req = server.takeRequest()
        assertEquals("PUT", req.method)
        assertEquals("/v1/users/me/my-spots/42/alarm", req.path)
        assertEquals("""{"enabled":true}""", req.body.readUtf8())
    }

    @Test
    fun `update enabled false sends body false and reflects in response`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"success":true,"code":"OK","message":"","data":{"spotId":7,"enabled":false}}"""
            )
        )

        val alarm = service.update(spotId = 7L, enabled = false)
        assertFalse(alarm.enabled)
        assertEquals("""{"enabled":false}""", server.takeRequest().body.readUtf8())
    }

    @Test
    fun `update propagates ApiException on failure`() {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"success":false,"code":"ALARM_FAIL","message":"forbidden","data":null}"""
            )
        )
        val ex = assertThrows(ApiException::class.java) {
            runBlocking { service.update(spotId = 1L, enabled = true) }
        }
        assertEquals("ALARM_FAIL", ex.code)
    }
}
