package com.pickflow.android.core.services.impl

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.pickflow.android.core.network.api.AddressApi
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import retrofit2.Retrofit

/** 주소 검색이 Kakao 직접 호출에서 서버 `/v1/address/search` 로 옮겨간 뒤의 매핑. */
class DefaultAddressServiceTest {

    private lateinit var server: MockWebServer
    private lateinit var service: DefaultAddressService

    @BeforeEach
    fun setUp() {
        server = MockWebServer().apply { start() }
        val json = Json { ignoreUnknownKeys = true; explicitNulls = false }
        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        service = DefaultAddressService(retrofit.create(AddressApi::class.java), mockk(), "")
    }

    @AfterEach
    fun tearDown() = server.shutdown()

    private fun respond(body: String) = server.enqueue(MockResponse().setBody(body))

    @Test
    fun `search maps the server response`() = runBlocking {
        respond(
            """
            {"success":true,"code":"S000","message":"성공","data":{"addresses":[
              {"addressName":"서울 강남구 강남대로 396","roadAddress":"서울 강남구 강남대로 396",
               "jibunAddress":"서울 강남구 역삼동 858","latitude":37.49,"longitude":127.02}
            ],"page":1,"totalCount":1,"isEnd":true}}
            """.trimIndent(),
        )

        val result = service.search("강남")

        assertEquals(1, result.size)
        assertEquals("서울 강남구 강남대로 396", result[0].fullAddress)
        assertEquals("서울 강남구 강남대로 396", result[0].name)
        assertEquals(37.49, result[0].latitude)
        val request = server.takeRequest()
        assertEquals("/v1/address/search?query=%EA%B0%95%EB%82%A8&size=30", request.path)
    }

    /** 좌표 없는 후보는 지도에 못 찍으니 버린다. */
    @Test
    fun `search drops candidates without coordinates`() = runBlocking {
        respond("""{"success":true,"data":{"addresses":[{"addressName":"서울 어딘가"}]}}""")

        assertTrue(service.search("서울").isEmpty())
    }

    /** 실패 응답은 빈 목록 폴백 — 검색창이 에러로 죽지 않는다. */
    @Test
    fun `search falls back to empty on failure`() = runBlocking {
        respond("""{"success":false,"code":"C001","message":"실패"}""")

        assertTrue(service.search("서울").isEmpty())
    }

    /** 빈 질의는 네트워크를 타지 않는다. */
    @Test
    fun `search short-circuits on a blank query`() = runBlocking {
        assertTrue(service.search("  ").isEmpty())
        assertEquals(0, server.requestCount)
    }
}
