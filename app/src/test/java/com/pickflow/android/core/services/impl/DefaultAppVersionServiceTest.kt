package com.pickflow.android.core.services.impl

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.pickflow.android.core.network.api.AppVersionApi
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

class DefaultAppVersionServiceTest {

    private lateinit var server: MockWebServer
    private lateinit var service: DefaultAppVersionService

    @BeforeEach
    fun setUp() {
        server = MockWebServer().apply { start() }
        val json = Json { ignoreUnknownKeys = true; explicitNulls = false }
        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        service = DefaultAppVersionService(retrofit.create(AppVersionApi::class.java))
    }

    @AfterEach
    fun tearDown() = server.shutdown()

    @Test
    fun `fetch uses android config and preserves its update policy`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {"success":true,"code":"OK","message":"","data":{
                  "minimumVersion":"1.0.4","latestVersion":"1.0.5","forceUpdate":true,
                  "storeUrl":"https://play.google.com/store/apps/details?id=com.pickflow.app",
                  "supportEmail":"support@pickflow.com",
                  "termsPolicies":[
                    {"type":"SERVICE","title":"서비스 이용약관","url":"https://pickflow.com/terms"}
                  ]
                }}
                """.trimIndent()
            )
        )

        val policy = service.fetchAndroidVersionPolicy()

        assertEquals("/v1/app/config/android", server.takeRequest().requestUrl!!.encodedPath)
        assertEquals("1.0.4", policy.minimumVersion)
        assertEquals("1.0.5", policy.latestVersion)
        assertTrue(policy.forceUpdate)
        assertEquals("https://play.google.com/store/apps/details?id=com.pickflow.app", policy.storeUrl)
        assertEquals("support@pickflow.com", policy.supportEmail)
        assertEquals("서비스 이용약관", policy.termsPolicies?.single()?.title)
    }
}
