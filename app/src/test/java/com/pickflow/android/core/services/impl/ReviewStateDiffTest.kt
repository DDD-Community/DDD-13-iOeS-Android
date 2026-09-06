package com.pickflow.android.core.services.impl

import com.pickflow.android.core.services.protocols.MySpot
import com.pickflow.android.core.services.protocols.MySpotStatus
import com.pickflow.android.core.services.protocols.ReviewDecision
import com.pickflow.android.core.services.protocols.SpotTheme
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** 검수 결과 감지는 목록 status 대조가 전부다 — 순수 함수라 여기서 다 검증한다. */
class ReviewStateDiffTest {

    @Test
    fun `first sight records status without emitting a result`() {
        val state = ReviewState().applying(listOf(spot(1L, MySpotStatus.PUBLISHED)))

        assertTrue(state.results.isEmpty())
        assertEquals(MySpotStatus.PUBLISHED.name, state.lastStatus["1"])
    }

    @Test
    fun `pending to published becomes an approval`() {
        val before = ReviewState().applying(listOf(spot(1L, MySpotStatus.PENDING)))

        val after = before.applying(listOf(spot(1L, MySpotStatus.PUBLISHED)))

        assertEquals(ReviewDecision.APPROVED.name, after.results.getValue("1").decision)
        assertTrue(!after.results.getValue("1").acknowledged)
    }

    @Test
    fun `re review pending to rejected becomes a rejection`() {
        val before = ReviewState().applying(listOf(spot(1L, MySpotStatus.RE_REVIEW_PENDING)))

        val after = before.applying(listOf(spot(1L, MySpotStatus.REJECTED)))

        assertEquals(ReviewDecision.REJECTED.name, after.results.getValue("1").decision)
    }

    @Test
    fun `unpublishing a spot is not a review result`() {
        val before = ReviewState().applying(listOf(spot(1L, MySpotStatus.PUBLISHED)))

        val after = before.applying(listOf(spot(1L, MySpotStatus.DRAFT)))

        assertTrue(after.results.isEmpty())
    }

    @Test
    fun `settled result survives the next poll until it is acknowledged`() {
        val approved = ReviewState()
            .applying(listOf(spot(1L, MySpotStatus.PENDING)))
            .applying(listOf(spot(1L, MySpotStatus.PUBLISHED)))

        val next = approved.applying(listOf(spot(1L, MySpotStatus.PUBLISHED)))

        assertEquals(approved.results.getValue("1"), next.results.getValue("1"))
    }

    @Test
    fun `deleted spot drops its result`() {
        val approved = ReviewState()
            .applying(listOf(spot(1L, MySpotStatus.PENDING), spot(2L, MySpotStatus.DRAFT)))
            .applying(listOf(spot(1L, MySpotStatus.PUBLISHED), spot(2L, MySpotStatus.DRAFT)))

        val next = approved.applying(listOf(spot(2L, MySpotStatus.DRAFT)))

        assertNull(next.results["1"])
        assertNull(next.lastStatus["1"])
    }

    @Test
    fun `stored result maps to the domain with the spot id as its result id`() {
        val stored = StoredResult(
            decision = ReviewDecision.APPROVED.name,
            occurredAt = "2026-09-04T00:00:00Z",
            acknowledged = true,
        )

        val result = stored.toDomain("41")

        assertEquals(41L, result.resultId)
        assertEquals(41L, result.spotId)
        assertEquals(ReviewDecision.APPROVED, result.decision)
        assertTrue(result.isAcknowledged)
        assertTrue(!result.publishedModalAcknowledged)
    }

    private fun spot(id: Long, status: MySpotStatus) = MySpot(
        id = id,
        name = "스팟 $id",
        theme = SpotTheme.SUNSET,
        imageUrl = null,
        latitude = 37.5,
        longitude = 127.0,
        distanceKm = null,
        createdAt = "2026-09-01T00:00:00Z",
        status = status,
        bookmarkCount = 0L,
    )
}
