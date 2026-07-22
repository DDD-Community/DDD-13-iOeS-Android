package com.pickflow.android.core.services.impl

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.pickflow.android.core.network.ApiException
import com.pickflow.android.core.network.api.SpotReportApi
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

class DefaultSpotReportServiceTest {

    private lateinit var server: MockWebServer
    private lateinit var service: DefaultSpotReportService

    @BeforeEach
    fun setUp() {
        server = MockWebServer().apply { start() }
        val json = Json { ignoreUnknownKeys = true; explicitNulls = false }
        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        service = DefaultSpotReportService(retrofit.create(SpotReportApi::class.java))
    }

    @AfterEach
    fun tearDown() = server.shutdown()

    @Test
    fun `report posts content to spot reports and returns reportId`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(201).setBody(
                """{"success":true,"code":"OK","message":"","data":{"reportId":777}}"""
            )
        )

        val id = service.report(spotId = 42L, content = "잘못된 위치 정보입니다 — 한 블록 옆이에요")

        assertEquals(777L, id)

        val req = server.takeRequest()
        assertEquals("POST", req.method)
        assertEquals("/v1/spots/42/reports", req.path)
        assertEquals(
            """{"content":"잘못된 위치 정보입니다 — 한 블록 옆이에요"}""",
            req.body.readUtf8()
        )
    }

    @Test
    fun `report succeeds even when data is null (iOS EmptyResponse parity)`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(201).setBody(
                """{"success":true,"code":"OK","message":"","data":null}"""
            )
        )

        val id = service.report(spotId = 42L, content = "실제 위치가 지도와 달라요")

        assertEquals(0L, id)
    }

    @Test
    fun `report propagates ApiException on validation failure (too short)`() {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"success":false,"code":"REPORT_LEN","message":"min 5 chars","data":null}"""
            )
        )
        val ex = assertThrows(ApiException::class.java) {
            runBlocking { service.report(spotId = 1L, content = "짧") }
        }
        assertEquals("REPORT_LEN", ex.code)
    }

    @Test
    fun `report long content within 200 chars succeeds`() = runBlocking {
        val long = "a".repeat(200)
        server.enqueue(
            MockResponse().setResponseCode(201).setBody(
                """{"success":true,"code":"OK","message":"","data":{"reportId":1}}"""
            )
        )

        val id = service.report(spotId = 1L, content = long)
        assertEquals(1L, id)

        val body = server.takeRequest().body.readUtf8()
        assertEquals("""{"content":"$long"}""", body)
    }
}
