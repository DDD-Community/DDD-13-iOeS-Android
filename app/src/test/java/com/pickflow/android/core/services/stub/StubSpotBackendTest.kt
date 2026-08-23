package com.pickflow.android.core.services.stub

import com.pickflow.android.core.services.protocols.Coordinates
import com.pickflow.android.core.services.protocols.ImagePayload
import com.pickflow.android.core.services.protocols.MySpotStatus
import com.pickflow.android.core.services.protocols.MySpotTransitionConflictException
import com.pickflow.android.core.services.protocols.RejectionReason
import com.pickflow.android.core.services.protocols.ReviewDecision
import com.pickflow.android.core.services.protocols.SavedSpotAvailability
import com.pickflow.android.core.services.protocols.SpotDraft
import com.pickflow.android.core.services.protocols.SpotSource
import com.pickflow.android.core.services.protocols.SpotTheme
import com.pickflow.android.core.services.protocols.ViewportBox
import kotlinx.coroutines.async
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StubSpotBackendTest {
    private val viewport = ViewportBox(
        topLeft = Coordinates(38.0, 126.0),
        topRight = Coordinates(38.0, 128.0),
        bottomLeft = Coordinates(36.0, 126.0),
        bottomRight = Coordinates(36.0, 128.0),
    )

    @Test
    fun `fixtures preserve all five my spot states`() = runTest {
        val services = services()

        val statuses = services.mySpot.list(page = 0).items.map { it.status }.toSet()

        assertEquals(MySpotStatus.entries.toSet(), statuses)
    }

    @Test
    fun `request open and cancel open are reflected by every shared service`() = runTest {
        val services = services()
        val draftId = StubSpotFixtures.DRAFT_SPOT_ID

        assertEquals(MySpotStatus.PENDING, services.mySpot.requestOpen(draftId).status)
        assertFalse(services.map.fetchInViewport(viewport).any { it.spotId == draftId })
        assertFalse(services.list.fetch(null, 0).items.any { it.id == draftId.toString() })

        services.backend.completeReview(draftId, ReviewDecision.APPROVED)

        assertTrue(services.map.fetchInViewport(viewport).any { it.spotId == draftId })
        assertTrue(services.list.fetch(null, 0).items.any { it.id == draftId.toString() })
        val unpublished = services.mySpot.unpublish(draftId)
        assertEquals(MySpotStatus.DRAFT, unpublished.status)
        assertEquals(MySpotStatus.PUBLISHED, unpublished.previousStatus)
        assertFalse(unpublished.wasOpenRequest)
        assertTrue(services.map.fetchInViewport(viewport).any { it.spotId == draftId })
        assertFalse(services.list.fetch(null, 0).items.any { it.id == draftId.toString() })
    }

    @Test
    fun `pending rereview and rejected withdrawals return to draft`() = runTest {
        val services = services()

        val fromPending = services.mySpot.unpublish(StubSpotFixtures.PENDING_SPOT_ID)
        assertEquals(MySpotStatus.DRAFT, fromPending.status)
        assertEquals(MySpotStatus.PENDING, fromPending.previousStatus)
        assertTrue(fromPending.wasOpenRequest)

        val fromReReview = services.mySpot.unpublish(StubSpotFixtures.RE_REVIEW_PENDING_SPOT_ID)
        assertEquals(MySpotStatus.DRAFT, fromReReview.status)
        assertEquals(MySpotStatus.RE_REVIEW_PENDING, fromReReview.previousStatus)
        assertTrue(fromReReview.wasOpenRequest)
    }

    @Test
    fun `create starts as draft and delete permanently removes it`() = runTest {
        val services = services()
        val draft = SpotDraft(
            name = "새 스팟",
            theme = SpotTheme.SUNSET,
            latitude = 37.5,
            longitude = 127.0,
            address = "서울",
        )
        val image = ImagePayload(byteArrayOf(1, 2, 3), "image/jpeg", "new.jpg")

        val created = services.mySpot.create(draft, image)

        assertEquals(MySpotStatus.DRAFT, created.status)
        assertEquals(MySpotStatus.DRAFT, services.mySpot.detail(created.spotId).status)
        services.mySpot.delete(created.spotId)
        assertFalse(services.mySpot.list(0).items.any { it.id == created.spotId })
        assertTrue(runCatching { services.mySpot.detail(created.spotId) }.isFailure)
    }

    @Test
    fun `revise without replacement keeps image and clears rejection`() = runTest {
        val services = services()
        val rejectedId = StubSpotFixtures.REJECTED_SPOT_ID
        val before = services.mySpot.detail(rejectedId)
        val revisedDraft = SpotDraft(
            name = "보완한 스팟",
            theme = SpotTheme.YUNSEUL,
            latitude = before.latitude,
            longitude = before.longitude,
            address = "보완한 주소",
            capturedDate = "2026-08-06",
            capturedTime = "19:20",
            comment = "보완한 코멘트",
        )

        // 수정만으로는 상태가 바뀌지 않고, 재신청에서 RE_REVIEW_PENDING 으로 전이된다.
        val updated = services.mySpot.update(rejectedId, revisedDraft, null)
        assertEquals(MySpotStatus.REJECTED, updated.status)

        val result = services.mySpot.requestOpen(rejectedId)
        val after = services.mySpot.detail(rejectedId)

        assertEquals(MySpotStatus.RE_REVIEW_PENDING, result.status)
        assertEquals(before.imageUrl, after.imageUrl)
        assertEquals("보완한 스팟", after.name)
        assertNull(after.rejection)
    }

    @Test
    fun `recommendation uses server final count and rejects private spot`() = runTest {
        val services = services()
        val publishedId = StubSpotFixtures.PUBLISHED_USER_SPOT_ID

        val recommended = services.recommendation.recommend(publishedId)
        val cancelled = services.recommendation.cancel(publishedId)

        assertTrue(recommended.isRecommended)
        assertEquals(recommended.recommendationCount - 1, cancelled.recommendationCount)
        assertFalse(cancelled.isRecommended)
        val privateFailure = runCatching {
            services.recommendation.recommend(StubSpotFixtures.DRAFT_SPOT_ID)
        }.exceptionOrNull()
        assertTrue(privateFailure is IllegalStateException)
    }

    @Test
    fun `cancel open preserves bookmark as author private`() = runTest {
        val services = services()
        val publishedId = StubSpotFixtures.PUBLISHED_USER_SPOT_ID
        val recommendation = services.recommendation.recommend(publishedId)

        services.mySpot.unpublish(publishedId)
        val saved = services.bookmark.savedSpots(page = 0).items.single { it.id == publishedId }

        assertEquals(SavedSpotAvailability.AUTHOR_PRIVATE, saved.availability)
        assertEquals(recommendation.recommendationCount, services.mySpot.detail(publishedId).recommendationCount)
        assertTrue(services.mySpot.detail(publishedId).isRecommended)
        services.bookmark.remove(publishedId.toString())
        assertFalse(services.bookmark.savedSpots(0).items.any { it.id == publishedId })
        assertNotNull(services.mySpot.detail(publishedId))
    }

    @Test
    fun `review completion updates state and acknowledges only selected result`() = runTest {
        val services = services()
        val pendingId = StubSpotFixtures.PENDING_SPOT_ID
        val rereviewId = StubSpotFixtures.RE_REVIEW_PENDING_SPOT_ID

        val approved = services.backend.completeReview(pendingId, ReviewDecision.APPROVED)
        val rejected = services.backend.completeReview(rereviewId, ReviewDecision.REJECTED, RejectionReason.LOW_QUALITY)

        val before = services.reviewResult.status()
        assertTrue(before.hasIndicator)
        assertEquals(2, before.unacknowledgedResults.size)
        services.reviewResult.acknowledge(approved.resultId)
        val after = services.reviewResult.status()

        assertEquals(
            listOf(approved.resultId, rejected.resultId),
            after.unacknowledgedResults.map { it.resultId },
        )
        assertTrue(after.hasIndicator)

        services.reviewResult.acknowledgePublishedModal(approved.resultId)
        assertEquals(
            listOf(rejected.resultId),
            services.reviewResult.status().unacknowledgedResults.map { it.resultId },
        )
    }

    @Test
    fun `configured failure leaves backend unchanged`() = runTest {
        val services = services()
        val id = StubSpotFixtures.DRAFT_SPOT_ID
        services.backend.enqueue(
            StubOperation.REQUEST_OPEN,
            StubResponse.Failure("deterministic failure"),
        )

        val failure = runCatching { services.mySpot.requestOpen(id) }.exceptionOrNull()
        assertTrue(failure is StubServiceException)
        assertEquals(MySpotStatus.DRAFT, services.mySpot.detail(id).status)
    }

    @Test
    fun `configured delay and review race are deterministic`() = runTest {
        val services = services()
        val id = StubSpotFixtures.PENDING_SPOT_ID
        services.backend.enqueue(StubOperation.UNPUBLISH, StubResponse.Delay(1_000))
        services.backend.enqueue(
            StubOperation.UNPUBLISH,
            StubResponse.ReviewRace(ReviewDecision.APPROVED),
        )

        val request = backgroundScope.async { runCatching { services.mySpot.unpublish(id) } }
        advanceTimeBy(999)
        assertFalse(request.isCompleted)
        advanceTimeBy(1)

        val failure = request.await().exceptionOrNull()
        assertTrue(failure is MySpotTransitionConflictException)
        failure as MySpotTransitionConflictException
        assertEquals(MySpotStatus.PUBLISHED, failure.latestStatus)
        assertEquals(MySpotStatus.PUBLISHED, services.mySpot.detail(id).status)
    }

    @Test
    fun `fixtures distinguish curated and user sources`() = runTest {
        val services = services()
        val markers = services.map.fetchInViewport(viewport)

        assertTrue(markers.any { it.source is SpotSource.Curated })
        assertTrue(markers.any { it.source == SpotSource.User })
        assertTrue(
            services.mySpot.detail(StubSpotFixtures.CURATED_SPOT_ID).source is SpotSource.Curated,
        )
    }

    private fun services(): Services {
        val backend = StubSpotBackend()
        return Services(
            backend = backend,
            mySpot = StubMySpotService(backend),
            recommendation = StubRecommendationService(backend),
            map = StubSpotMapService(backend),
            list = StubSpotListService(backend),
            bookmark = StubBookmarkService(backend),
            reviewResult = StubReviewResultService(backend),
        )
    }

    private data class Services(
        val backend: StubSpotBackend,
        val mySpot: StubMySpotService,
        val recommendation: StubRecommendationService,
        val map: StubSpotMapService,
        val list: StubSpotListService,
        val bookmark: StubBookmarkService,
        val reviewResult: StubReviewResultService,
    )
}
