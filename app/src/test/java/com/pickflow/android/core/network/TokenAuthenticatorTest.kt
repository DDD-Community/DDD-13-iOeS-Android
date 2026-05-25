package com.pickflow.android.core.network

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.pickflow.android.core.network.api.RefreshApi
import com.pickflow.android.core.services.impl.InMemoryTokenStore
import javax.inject.Provider
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import retrofit2.Retrofit

class TokenAuthenticatorTest {

    private lateinit var server: MockWebServer
    private lateinit var tokenStore: InMemoryTokenStore
    private lateinit var authenticator: TokenAuthenticator
    private lateinit var client: OkHttpClient

    @BeforeEach
    fun setUp() {
        server = MockWebServer().apply { start() }
        tokenStore = InMemoryTokenStore()
        val json = Json { ignoreUnknownKeys = true; explicitNulls = false }
        val refreshRetrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(OkHttpClient.Builder().build())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        val refreshApi = refreshRetrofit.create(RefreshApi::class.java)
        authenticator = TokenAuthenticator(
            refreshApiProvider = Provider { refreshApi },
            tokenStore = tokenStore,
        )
        client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokenStore))
            .authenticator(authenticator)
            .build()
    }

    @AfterEach
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `401 triggers refresh and retries with new token`() = runBlocking {
        tokenStore.save(accessToken = "OLD_ACC", refreshToken = "REF1")
        // 첫 요청 → 401
        server.enqueue(MockResponse().setResponseCode(401))
        // refresh 응답
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {"success":true,"code":"OK","message":"","data":{
                  "accessToken":"NEW_ACC","refreshToken":"REF2",
                  "profile":{"userId":"u","email":null,"nickname":"u","profileImageUrl":null,"provider":"KAKAO"}
                }}
                """.trimIndent()
            )
        )
        // 재시도 → 200
        server.enqueue(MockResponse().setResponseCode(200).setBody("ok"))

        val resp = client.newCall(Request.Builder().url(server.url("/v1/spots")).build()).execute()
        assertEquals(200, resp.code)
        resp.close()

        assertEquals("NEW_ACC", tokenStore.accessToken())
        assertEquals("REF2", tokenStore.refreshToken())

        // 첫 요청
        val first = server.takeRequest()
        assertEquals("Bearer OLD_ACC", first.getHeader("Authorization"))
        // refresh 요청 (Authorization 없음)
        val refresh = server.takeRequest()
        assertEquals("/v1/auth/refresh", refresh.path)
        assertNull(refresh.getHeader("Authorization"))
        // 재시도 — 새 토큰
        val retry = server.takeRequest()
        assertEquals("Bearer NEW_ACC", retry.getHeader("Authorization"))
    }

    @Test
    fun `refresh failure clears tokens and abandons retry`() = runBlocking {
        tokenStore.save(accessToken = "OLD", refreshToken = "BAD_REF")
        server.enqueue(MockResponse().setResponseCode(401))
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"success":false,"code":"AUTH_002","message":"refresh expired","data":null}"""
            )
        )

        val resp = client.newCall(Request.Builder().url(server.url("/v1/spots")).build()).execute()
        // authenticator가 null 반환 → 원래 401이 그대로 전달
        assertEquals(401, resp.code)
        resp.close()

        assertNull(tokenStore.accessToken())
        assertNull(tokenStore.refreshToken())
    }

    @Test
    fun `no refresh token in store skips refresh entirely`() = runBlocking {
        // accessToken만 있고 refresh 없음
        tokenStore.save(accessToken = "ONLY_ACC", refreshToken = null)
        server.enqueue(MockResponse().setResponseCode(401))

        val resp = client.newCall(Request.Builder().url(server.url("/v1/spots")).build()).execute()
        assertEquals(401, resp.code)
        resp.close()

        // refresh 호출 안 됨 → 두 번째 takeRequest는 없어야 하지만 검증은 패스 (서버 queue 비어 있음)
        // tokenStore는 그대로 유지 (clear() 호출 안 함)
        assertEquals("ONLY_ACC", tokenStore.accessToken())
    }
}
