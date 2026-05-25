package com.pickflow.android.core.network

import com.pickflow.android.core.services.impl.InMemoryTokenStore
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AuthInterceptorTest {

    private lateinit var server: MockWebServer
    private lateinit var tokenStore: InMemoryTokenStore
    private lateinit var client: OkHttpClient

    @BeforeEach
    fun setUp() {
        server = MockWebServer().apply { start() }
        tokenStore = InMemoryTokenStore()
        client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokenStore))
            .build()
    }

    @AfterEach
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `attaches Bearer header when token is present`() = runBlocking {
        tokenStore.save(accessToken = "ACCESS_123", refreshToken = "REFRESH_xyz")
        server.enqueue(MockResponse().setResponseCode(200))

        client.newCall(Request.Builder().url(server.url("/v1/spots")).build()).execute().close()

        val recorded = server.takeRequest()
        assertEquals("Bearer ACCESS_123", recorded.getHeader("Authorization"))
    }

    @Test
    fun `does not attach header when token is missing`() {
        server.enqueue(MockResponse().setResponseCode(200))

        client.newCall(Request.Builder().url(server.url("/v1/auth/kakao")).build()).execute().close()

        val recorded = server.takeRequest()
        assertNull(recorded.getHeader("Authorization"))
    }

    @Test
    fun `preserves explicit Authorization header if caller already set it`() = runBlocking {
        tokenStore.save(accessToken = "ACCESS_123", refreshToken = null)
        server.enqueue(MockResponse().setResponseCode(200))

        val req = Request.Builder()
            .url(server.url("/v1/spots"))
            .header("Authorization", "Bearer EXPLICIT")
            .build()
        client.newCall(req).execute().close()

        val recorded = server.takeRequest()
        assertEquals("Bearer EXPLICIT", recorded.getHeader("Authorization"))
    }
}
