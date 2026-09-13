package com.pickflow.android.core.services.impl

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.pickflow.android.core.network.api.BookmarkApi
import com.pickflow.android.core.services.protocols.SavedSpotAvailability
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import retrofit2.Retrofit

/**
 * 저장된 스팟의 `deleted` / `isReleased` → [SavedSpotAvailability] 매핑.
 * 노출 판단은 `isPrivate` 가 아니라 `isReleased` 다 — `isPrivate` 는 검수 상태만 보고
 * 등록자가 노출을 끈 PUBLISHED 스팟을 놓친다(스웨거 명시).
 */
class DefaultBookmarkServiceAvailabilityTest {

    private lateinit var server: MockWebServer
    private lateinit var service: DefaultBookmarkService

    @BeforeEach
    fun setUp() {
        server = MockWebServer().apply { start() }
        val json = Json { ignoreUnknownKeys = true; explicitNulls = false }
        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        service = DefaultBookmarkService(retrofit.create(BookmarkApi::class.java))
    }

    @AfterEach
    fun tearDown() = server.shutdown()

    @Test
    fun `deleted, 노출꺼짐, 정상 스팟이 각각 다른 availability 로 매핑된다`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {
                  "success": true, "code": "S000", "message": "ok",
                  "data": {
                    "spots": [
                      {"spotId":1,"name":"삭제됨","theme":"SUNSET","imageUrl":null,
                       "latitude":37.5,"longitude":127.0,"savedAt":"2026-08-01T00:00:00Z",
                       "deleted":true,"isReleased":true},
                      {"spotId":2,"name":"작성자 노출 끔","theme":"NIGHT_VIEW","imageUrl":null,
                       "latitude":37.5,"longitude":127.0,"savedAt":"2026-08-02T00:00:00Z",
                       "deleted":false,"isReleased":false},
                      {"spotId":3,"name":"정상","theme":"SUNSET","imageUrl":"https://img/3.jpg",
                       "latitude":37.5,"longitude":127.0,"savedAt":"2026-08-03T00:00:00Z",
                       "deleted":false,"isReleased":true}
                    ],
                    "page": 0, "hasNext": false
                  }
                }
                """.trimIndent(),
            ),
        )

        val page = service.savedSpots(page = 0)

        assertEquals(SavedSpotAvailability.DELETED, page.items[0].availability)
        assertEquals(SavedSpotAvailability.AUTHOR_PRIVATE, page.items[1].availability)
        assertEquals(SavedSpotAvailability.AVAILABLE, page.items[2].availability)
    }

    @Test
    fun `삭제되고 노출도 꺼져 있으면 삭제가 우선한다`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {
                  "success": true, "code": "S000", "message": "ok",
                  "data": {
                    "spots": [
                      {"spotId":9,"name":"둘 다","theme":"SUNSET","imageUrl":null,
                       "latitude":37.5,"longitude":127.0,"savedAt":"2026-08-04T00:00:00Z",
                       "deleted":true,"isReleased":false}
                    ],
                    "page": 0, "hasNext": false
                  }
                }
                """.trimIndent(),
            ),
        )

        assertEquals(SavedSpotAvailability.DELETED, service.savedSpots(page = 0).items.single().availability)
    }

    @Test
    fun `isReleased 필드가 없는 응답은 AVAILABLE 로 떨어진다`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {
                  "success": true, "code": "S000", "message": "ok",
                  "data": {
                    "spots": [
                      {"spotId":7,"name":"구 응답","theme":"SUNSET","imageUrl":"https://img/7.jpg",
                       "latitude":37.5,"longitude":127.0,"savedAt":"2026-08-05T00:00:00Z"}
                    ],
                    "page": 0, "hasNext": false
                  }
                }
                """.trimIndent(),
            ),
        )

        assertEquals(SavedSpotAvailability.AVAILABLE, service.savedSpots(page = 0).items.single().availability)
    }

    @Test
    fun `isPrivate 가 true 여도 isReleased 가 true 면 노출된다`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {
                  "success": true, "code": "S000", "message": "ok",
                  "data": {
                    "spots": [
                      {"spotId":11,"name":"검수중이나 노출 켜짐","theme":"SUNSET","imageUrl":"https://img/11.jpg",
                       "latitude":37.5,"longitude":127.0,"savedAt":"2026-08-06T00:00:00Z",
                       "deleted":false,"isPrivate":true,"isReleased":true}
                    ],
                    "page": 0, "hasNext": false
                  }
                }
                """.trimIndent(),
            ),
        )

        assertEquals(SavedSpotAvailability.AVAILABLE, service.savedSpots(page = 0).items.single().availability)
    }

    @Test
    fun `isPrivate 가 false 여도 isReleased 가 false 면 비공개다`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {
                  "success": true, "code": "S000", "message": "ok",
                  "data": {
                    "spots": [
                      {"spotId":12,"name":"공개됐지만 노출 끔","theme":"SUNSET","imageUrl":"https://img/12.jpg",
                       "latitude":37.5,"longitude":127.0,"savedAt":"2026-08-07T00:00:00Z",
                       "deleted":false,"isPrivate":false,"isReleased":false}
                    ],
                    "page": 0, "hasNext": false
                  }
                }
                """.trimIndent(),
            ),
        )

        assertEquals(
            SavedSpotAvailability.AUTHOR_PRIVATE,
            service.savedSpots(page = 0).items.single().availability,
        )
    }
}
