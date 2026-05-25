package com.pickflow.android.core.services.impl

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.pickflow.android.core.network.ApiException
import com.pickflow.android.core.network.api.AuthApi
import com.pickflow.android.core.services.protocols.SocialAuthCredential
import com.pickflow.android.core.services.protocols.SocialProvider
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

class DefaultSocialLoginServiceTest {

    private lateinit var server: MockWebServer
    private lateinit var api: AuthApi
    private lateinit var tokenStore: InMemoryTokenStore
    private lateinit var service: DefaultSocialLoginService

    @BeforeEach
    fun setUp() {
        server = MockWebServer().apply { start() }
        val json = Json { ignoreUnknownKeys = true; explicitNulls = false }
        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        api = retrofit.create(AuthApi::class.java)
        tokenStore = InMemoryTokenStore()
        service = DefaultSocialLoginService(api, tokenStore)
    }

    @AfterEach
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `kakao login returns AuthenticatedSession and persists tokens`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {
                  "success": true,
                  "code": "OK",
                  "message": "",
                  "data": {
                    "accessToken": "ACC",
                    "refreshToken": "REF",
                    "profile": {
                      "userId": "u1",
                      "email": "kdy@example.com",
                      "nickname": "pickflower",
                      "profileImageUrl": "https://img/u1.png",
                      "provider": "KAKAO"
                    }
                  }
                }
                """.trimIndent()
            )
        )

        val session = service.loginWith(
            SocialAuthCredential(SocialProvider.KAKAO, "kakao-access", null)
        )

        assertEquals("ACC", session.tokens.accessToken)
        assertEquals("REF", session.tokens.refreshToken)
        assertEquals("u1", session.profile.userId)
        assertEquals(SocialProvider.KAKAO, session.profile.provider)
        assertEquals("ACC", tokenStore.accessToken())
        assertEquals("REF", tokenStore.refreshToken())

        val req = server.takeRequest()
        assertEquals("POST", req.method)
        assertEquals("/v1/auth/kakao", req.path)
        assertEquals(
            """{"accessToken":"kakao-access"}""",
            req.body.readUtf8()
        )
    }

    @Test
    fun `kakao login propagates ApiException on success=false and does not save tokens`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"success":false,"code":"AUTH_001","message":"카카오 토큰 무효","data":null}"""
            )
        )

        val ex = assertThrows(ApiException::class.java) {
            runBlocking {
                service.loginWith(SocialAuthCredential(SocialProvider.KAKAO, "bad", null))
            }
        }
        assertEquals("AUTH_001", ex.code)
        assertEquals(null, tokenStore.accessToken())
    }

    @Test
    fun `apple login currently throws IllegalStateException until next iteration`() {
        assertThrows(IllegalStateException::class.java) {
            runBlocking {
                service.loginWith(SocialAuthCredential(SocialProvider.APPLE, "id-token", null))
            }
        }
    }
}
