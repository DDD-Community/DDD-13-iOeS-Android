package com.pickflow.android.core.services.impl

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.pickflow.android.core.network.ApiException
import com.pickflow.android.core.network.api.AuthApi
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

class DefaultAuthServiceTest {

    private lateinit var server: MockWebServer
    private lateinit var api: AuthApi
    private lateinit var userApi: UserApi
    private lateinit var tokenStore: InMemoryTokenStore
    private lateinit var service: DefaultAuthService

    @BeforeEach
    fun setUp() {
        server = MockWebServer().apply { start() }
        val json = Json { ignoreUnknownKeys = true; explicitNulls = false }
        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        api = retrofit.create(AuthApi::class.java)
        userApi = retrofit.create(UserApi::class.java)
        tokenStore = InMemoryTokenStore()
        service = DefaultAuthService(api, userApi, tokenStore)
    }

    @AfterEach
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `logout calls server with refreshToken and clears tokens on success`() = runBlocking {
        tokenStore.save(accessToken = "ACC", refreshToken = "REF")
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setBody("""{"success":true,"code":"OK","message":"","data":null}""")
        )

        service.logout()

        assertNull(tokenStore.accessToken())
        assertNull(tokenStore.refreshToken())

        val req = server.takeRequest()
        assertEquals("POST", req.method)
        assertEquals("/v1/auth/logout", req.path)
        assertEquals("""{"refreshToken":"REF"}""", req.body.readUtf8())
    }

    @Test
    fun `logout still clears local tokens when server returns failure`() = runBlocking {
        tokenStore.save(accessToken = "ACC", refreshToken = "REF")
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setBody("""{"success":false,"code":"AUTH_003","message":"invalid","data":null}""")
        )

        val ex = assertThrows(ApiException::class.java) {
            runBlocking { service.logout() }
        }
        assertEquals("AUTH_003", ex.code)
        // finally 블록에서 토큰은 클리어됨
        assertNull(tokenStore.accessToken())
        assertNull(tokenStore.refreshToken())
    }

    @Test
    fun `logout without stored refresh token only clears locally and does not call server`() = runBlocking {
        tokenStore.save(accessToken = "ACC_ONLY", refreshToken = null)
        // 서버에 enqueue 안 함 — 호출되면 dispatcher가 503을 반환할 것

        service.logout()

        assertNull(tokenStore.accessToken())
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `isLoggedIn reflects token presence`() = runBlocking {
        assertEquals(false, service.isLoggedIn())
        tokenStore.save(accessToken = "A", refreshToken = null)
        assertEquals(true, service.isLoggedIn())
    }

    @Test
    fun `withdraw calls DELETE users me and clears tokens on success`() = runBlocking {
        tokenStore.save(accessToken = "ACC", refreshToken = "REF")
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setBody("""{"success":true,"code":"OK","message":"","data":null}""")
        )

        service.withdraw()

        assertNull(tokenStore.accessToken())
        assertNull(tokenStore.refreshToken())

        val req = server.takeRequest()
        assertEquals("DELETE", req.method)
        assertEquals("/v1/users/me", req.path)
    }

    @Test
    fun `restore calls PATCH users restore with token query`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setBody("""{"success":true,"code":"OK","message":"","data":null}""")
        )

        service.restore(restoreToken = "RST-abc123")

        val req = server.takeRequest()
        assertEquals("PATCH", req.method)
        val url = req.requestUrl!!
        assertEquals("/v1/users/restore", url.encodedPath)
        assertEquals("RST-abc123", url.queryParameter("restoreToken"))
    }

    @Test
    fun `restore propagates ApiException on failure`() {
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setBody("""{"success":false,"code":"USR_RESTORE","message":"expired","data":null}""")
        )
        val ex = assertThrows(ApiException::class.java) {
            runBlocking { service.restore("bad") }
        }
        assertEquals("USR_RESTORE", ex.code)
    }

    @Test
    fun `withdraw still clears local tokens when server returns failure`() = runBlocking {
        tokenStore.save(accessToken = "ACC", refreshToken = "REF")
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setBody("""{"success":false,"code":"USR_FORBIDDEN","message":"cannot delete","data":null}""")
        )

        val ex = assertThrows(ApiException::class.java) {
            runBlocking { service.withdraw() }
        }
        assertEquals("USR_FORBIDDEN", ex.code)
        assertNull(tokenStore.accessToken())
        assertNull(tokenStore.refreshToken())
    }
}
