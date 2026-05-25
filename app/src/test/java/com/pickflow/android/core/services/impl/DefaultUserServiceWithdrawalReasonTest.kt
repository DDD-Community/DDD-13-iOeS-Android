package com.pickflow.android.core.services.impl

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.pickflow.android.core.network.ApiException
import com.pickflow.android.core.network.api.UserApi
import com.pickflow.android.core.services.protocols.WithdrawalReasonType
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

class DefaultUserServiceWithdrawalReasonTest {

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
    fun `saveWithdrawalReason with content posts both fields`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setBody("""{"success":true,"code":"OK","message":"","data":null}""")
        )

        service.saveWithdrawalReason(WithdrawalReasonType.OTHERS, content = "기능 부족")

        val req = server.takeRequest()
        assertEquals("POST", req.method)
        assertEquals("/v1/users/me/withdrawal-reason", req.path)
        assertEquals(
            """{"reasonType":"OTHERS","content":"기능 부족"}""",
            req.body.readUtf8()
        )
    }

    @Test
    fun `saveWithdrawalReason without content omits content field`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setBody("""{"success":true,"code":"OK","message":"","data":null}""")
        )

        service.saveWithdrawalReason(WithdrawalReasonType.OTHERS)

        val req = server.takeRequest()
        // explicitNulls=false 로 null content 는 직렬화에서 제외
        assertEquals("""{"reasonType":"OTHERS"}""", req.body.readUtf8())
    }

    @Test
    fun `saveWithdrawalReason propagates ApiException on success=false`() {
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setBody("""{"success":false,"code":"USR_REASON","message":"too long","data":null}""")
        )
        val ex = assertThrows(ApiException::class.java) {
            runBlocking { service.saveWithdrawalReason(WithdrawalReasonType.OTHERS, "x".repeat(300)) }
        }
        assertEquals("USR_REASON", ex.code)
    }
}
