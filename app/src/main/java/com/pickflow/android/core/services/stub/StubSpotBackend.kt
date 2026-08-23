package com.pickflow.android.core.services.stub

import com.pickflow.android.core.services.protocols.Coordinates
import com.pickflow.android.core.services.protocols.CreateMySpotResult
import com.pickflow.android.core.services.protocols.ImagePayload
import com.pickflow.android.core.services.protocols.MySpot
import com.pickflow.android.core.services.protocols.MySpotDetail
import com.pickflow.android.core.services.protocols.MySpotPage
import com.pickflow.android.core.services.protocols.MySpotStatus
import com.pickflow.android.core.services.protocols.MySpotTransitionConflictException
import com.pickflow.android.core.services.protocols.MySpotTransitionResult
import com.pickflow.android.core.services.protocols.MySpotUnpublishResult
import com.pickflow.android.core.services.protocols.MySpotUpdateResult
import com.pickflow.android.core.services.protocols.RecommendationResult
import com.pickflow.android.core.services.protocols.RejectionReason
import com.pickflow.android.core.services.protocols.ReviewDecision
import com.pickflow.android.core.services.protocols.ReviewResult
import com.pickflow.android.core.services.protocols.ReviewResultStatus
import com.pickflow.android.core.services.protocols.SavedSpot
import com.pickflow.android.core.services.protocols.SavedSpotAvailability
import com.pickflow.android.core.services.protocols.SavedSpotPage
import com.pickflow.android.core.services.protocols.Spot
import com.pickflow.android.core.services.protocols.SpotDraft
import com.pickflow.android.core.services.protocols.SpotMapMarker
import com.pickflow.android.core.services.protocols.SpotPage
import com.pickflow.android.core.services.protocols.SpotSort
import com.pickflow.android.core.services.protocols.SpotTheme
import com.pickflow.android.core.services.protocols.ViewportBox
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.hypot
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Singleton
class StubSpotBackend @Inject constructor() {
    private val stateMutex = Mutex()
    private val responseLock = Any()
    private val records = linkedMapOf<Long, StubSpotRecord>()
    private val reviewResults = linkedMapOf<Long, ReviewResult>()
    private val responses = mutableMapOf<StubOperation, ArrayDeque<StubResponse>>()
    private var nextSpotId = 42_000L
    private var nextReviewResultId = 1L
    private var clockTick = 0L

    init {
        resetState()
    }

    suspend fun reset(scenario: StubScenario = StubScenario.Success) {
        stateMutex.withLock { resetState() }
        configure(scenario)
    }

    fun configure(scenario: StubScenario) {
        synchronized(responseLock) {
            responses.clear()
            scenario.responses.forEach { (operation, configured) ->
                responses[operation] = ArrayDeque(configured)
            }
        }
    }

    fun enqueue(operation: StubOperation, response: StubResponse) {
        synchronized(responseLock) {
            responses.getOrPut(operation) { ArrayDeque() }.addLast(response)
        }
    }

    internal suspend fun mySpots(page: Int, coordinates: Coordinates?): MySpotPage {
        before(StubOperation.MY_SPOT_LIST)
        return stateMutex.withLock {
            val items = if (page == 0) {
                records.values
                    .filter { it.isOwnedByCurrentUser && !it.isDeleted }
                    .sortedBy { it.id }
                    .map { it.toMySpot(coordinates) }
            } else {
                emptyList()
            }
            MySpotPage(items = items, page = page, hasNext = false)
        }
    }

    internal suspend fun detail(spotId: Long): MySpotDetail {
        before(StubOperation.MY_SPOT_DETAIL, spotId)
        return stateMutex.withLock {
            records[spotId]
                ?.takeUnless { it.isDeleted }
                ?.toMySpotDetail()
                ?: throw NoSuchElementException("spot $spotId does not exist")
        }
    }

    internal suspend fun create(draft: SpotDraft, image: ImagePayload): CreateMySpotResult {
        before(StubOperation.CREATE)
        return stateMutex.withLock {
            val id = nextSpotId++
            val updatedAt = now()
            records[id] = StubSpotRecord(
                id = id,
                name = draft.name,
                theme = draft.theme,
                imageUrl = "stub://images/${image.filename}",
                latitude = draft.latitude,
                longitude = draft.longitude,
                address = draft.address,
                capturedDate = draft.capturedDate,
                capturedTime = draft.capturedTime,
                comment = draft.comment,
                status = MySpotStatus.DRAFT,
                rejection = null,
                recommendationCount = 0,
                isRecommended = false,
                source = com.pickflow.android.core.services.protocols.SpotSource.User,
                isOwnedByCurrentUser = true,
                bookmarked = false,
                bookmarkCount = 0,
                createdAt = updatedAt,
                updatedAt = updatedAt,
            )
            CreateMySpotResult(spotId = id, status = MySpotStatus.DRAFT, imageUrl = records[id]?.imageUrl)
        }
    }

    /** DRAFT -> PENDING, REJECTED -> RE_REVIEW_PENDING. 재신청 시 반려 정보를 지운다. */
    internal suspend fun requestOpen(spotId: Long): MySpotTransitionResult {
        before(StubOperation.REQUEST_OPEN, spotId)
        return stateMutex.withLock {
            val record = ownedRecord(spotId)
            requireStatus(record, setOf(MySpotStatus.DRAFT, MySpotStatus.REJECTED))
            val wasRejected = record.status == MySpotStatus.REJECTED
            record.status = if (wasRejected) {
                MySpotStatus.RE_REVIEW_PENDING
            } else {
                MySpotStatus.PENDING
            }
            if (wasRejected) record.rejection = null
            record.updatedAt = now()
            record.toTransitionResult()
        }
    }

    /**
     * 공개 해제. 검수중이면 철회, 공개면 비공개 전환이며 둘 다 DRAFT 로 되돌린다.
     * DRAFT 는 해제할 대상이 없다.
     */
    internal suspend fun unpublish(spotId: Long): MySpotUnpublishResult {
        before(StubOperation.UNPUBLISH, spotId)
        return stateMutex.withLock {
            val record = ownedRecord(spotId)
            requireStatus(
                record,
                setOf(MySpotStatus.PENDING, MySpotStatus.RE_REVIEW_PENDING, MySpotStatus.PUBLISHED),
            )
            val previousStatus = record.status
            record.status = MySpotStatus.DRAFT
            record.updatedAt = now()
            MySpotUnpublishResult(
                spotId = record.id,
                previousStatus = previousStatus,
                status = record.status,
                updatedAt = record.updatedAt,
            )
        }
    }

    /** 수정만 수행한다. 상태는 그대로 두고, 이미지 미첨부 시 기존 이미지를 유지한다. */
    internal suspend fun update(
        spotId: Long,
        draft: SpotDraft,
        replacementImage: ImagePayload?,
    ): MySpotUpdateResult {
        before(StubOperation.UPDATE, spotId)
        return stateMutex.withLock {
            val record = ownedRecord(spotId)
            requireStatus(record, setOf(MySpotStatus.DRAFT, MySpotStatus.REJECTED))
            record.name = draft.name
            record.theme = draft.theme
            record.latitude = draft.latitude
            record.longitude = draft.longitude
            record.address = draft.address
            record.capturedDate = draft.capturedDate
            record.capturedTime = draft.capturedTime
            record.comment = draft.comment
            replacementImage?.let { record.imageUrl = "stub://images/${it.filename}" }
            record.updatedAt = now()
            MySpotUpdateResult(
                spotId = record.id,
                status = record.status,
                imageUrl = record.imageUrl,
            )
        }
    }

    internal suspend fun delete(spotId: Long) {
        before(StubOperation.DELETE, spotId)
        stateMutex.withLock {
            ownedRecord(spotId)
            records.remove(spotId)
            reviewResults.entries.removeAll { it.value.spotId == spotId }
        }
    }

    internal suspend fun recommend(spotId: Long): RecommendationResult {
        before(StubOperation.RECOMMEND, spotId)
        return stateMutex.withLock {
            val record = publicRecord(spotId)
            if (!record.isRecommended) {
                record.isRecommended = true
                record.recommendationCount += 1
            }
            record.toRecommendationResult()
        }
    }

    internal suspend fun cancelRecommendation(spotId: Long): RecommendationResult {
        before(StubOperation.CANCEL_RECOMMENDATION, spotId)
        return stateMutex.withLock {
            val record = publicRecord(spotId)
            if (record.isRecommended) {
                record.isRecommended = false
                record.recommendationCount = (record.recommendationCount - 1).coerceAtLeast(0)
            }
            record.toRecommendationResult()
        }
    }

    internal suspend fun markers(box: ViewportBox, theme: SpotTheme?): List<SpotMapMarker> {
        before(StubOperation.MAP)
        return stateMutex.withLock {
            val latitudes = listOf(
                box.topLeft.latitude,
                box.topRight.latitude,
                box.bottomLeft.latitude,
                box.bottomRight.latitude,
            )
            val longitudes = listOf(
                box.topLeft.longitude,
                box.topRight.longitude,
                box.bottomLeft.longitude,
                box.bottomRight.longitude,
            )
            records.values
                .filter { !it.isDeleted }
                .filter { it.theme == theme || theme == null }
                .filter { it.latitude in latitudes.min()..latitudes.max() }
                .filter { it.longitude in longitudes.min()..longitudes.max() }
                .filter {
                    it.status == MySpotStatus.PUBLISHED ||
                        (it.isOwnedByCurrentUser && it.status == MySpotStatus.DRAFT)
                }
                .sortedBy { it.id }
                .map {
                    SpotMapMarker(
                        spotId = it.id,
                        imageUrl = it.imageUrl,
                        coordinates = Coordinates(it.latitude, it.longitude),
                        isMySpot = it.isOwnedByCurrentUser,
                        source = it.source,
                        status = it.status,
                        isOwnedByCurrentUser = it.isOwnedByCurrentUser,
                    )
                }
        }
    }

    internal suspend fun publicSpots(
        theme: SpotTheme?,
        page: Int,
        coordinates: Coordinates?,
        sort: SpotSort,
    ): SpotPage {
        before(StubOperation.SPOT_LIST)
        return stateMutex.withLock {
            var items = if (page == 0) {
                records.values.filter {
                    !it.isDeleted && it.status == MySpotStatus.PUBLISHED && (theme == null || it.theme == theme)
                }
            } else {
                emptyList()
            }
            items = when (sort) {
                SpotSort.DISTANCE -> items.sortedBy { coordinates?.let(it::distanceFrom) ?: Double.MAX_VALUE }
                SpotSort.RECOMMENDED -> items.sortedByDescending { it.recommendationCount }
            }
            SpotPage(
                items = items.map { it.toSpot(coordinates) },
                page = page,
                hasNext = false,
            )
        }
    }

    internal suspend fun addBookmark(spotId: Long): Long {
        before(StubOperation.BOOKMARK_ADD, spotId)
        return stateMutex.withLock {
            val record = publicRecord(spotId)
            if (!record.bookmarked) {
                record.bookmarked = true
                record.bookmarkCount += 1
            }
            record.bookmarkCount
        }
    }

    internal suspend fun removeBookmark(spotId: Long): Long {
        before(StubOperation.BOOKMARK_REMOVE, spotId)
        return stateMutex.withLock {
            val record = records[spotId] ?: throw NoSuchElementException("spot $spotId does not exist")
            if (record.bookmarked) {
                record.bookmarked = false
                record.bookmarkCount = (record.bookmarkCount - 1).coerceAtLeast(0)
            }
            record.bookmarkCount
        }
    }

    internal suspend fun savedSpots(page: Int, coordinates: Coordinates?): SavedSpotPage {
        before(StubOperation.SAVED_SPOTS)
        return stateMutex.withLock {
            val items = if (page == 0) {
                records.values.filter { it.bookmarked }.sortedBy { it.id }.map { it.toSavedSpot(coordinates) }
            } else {
                emptyList()
            }
            SavedSpotPage(items = items, page = page, hasNext = false)
        }
    }

    internal suspend fun isBookmarked(spotId: Long): Boolean =
        stateMutex.withLock { records[spotId]?.bookmarked == true }

    internal suspend fun bookmarkedIds(): Set<String> = stateMutex.withLock {
        records.values.filter { it.bookmarked }.mapTo(linkedSetOf()) { it.id.toString() }
    }

    suspend fun completeReview(
        spotId: Long,
        decision: ReviewDecision,
        rejectionReason: RejectionReason? = null,
        rejectionDetail: String? = null,
    ): ReviewResult = stateMutex.withLock {
        val record = ownedRecord(spotId)
        requireStatus(record, setOf(MySpotStatus.PENDING, MySpotStatus.RE_REVIEW_PENDING))
        record.status = when (decision) {
            ReviewDecision.APPROVED -> MySpotStatus.PUBLISHED
            ReviewDecision.REJECTED -> MySpotStatus.REJECTED
        }
        record.updatedAt = now()
        record.rejection = if (decision == ReviewDecision.REJECTED) {
            StubRejections.of(
                reason = rejectionReason ?: RejectionReason.LOW_QUALITY,
                detail = rejectionDetail,
                rejectedAt = record.updatedAt,
            )
        } else {
            null
        }
        val result = ReviewResult(
            resultId = nextReviewResultId++,
            spotId = spotId,
            decision = decision,
            occurredAt = record.updatedAt,
        )
        reviewResults[result.resultId] = result
        result
    }

    internal suspend fun reviewStatus(): ReviewResultStatus {
        before(StubOperation.REVIEW_STATUS)
        return stateMutex.withLock {
            ReviewResultStatus(
                pendingRequestCount = records.values.count {
                    !it.isDeleted &&
                        (it.status == MySpotStatus.PENDING || it.status == MySpotStatus.RE_REVIEW_PENDING)
                },
                unacknowledgedResults = reviewResults.values.filter { result ->
                    !result.isAcknowledged ||
                        result.decision == ReviewDecision.APPROVED &&
                        !result.publishedModalAcknowledged
                },
            )
        }
    }

    internal suspend fun acknowledgeReview(resultId: Long) {
        before(StubOperation.ACKNOWLEDGE_REVIEW)
        stateMutex.withLock {
            val result = reviewResults[resultId]
                ?: throw NoSuchElementException("review result $resultId does not exist")
            reviewResults[resultId] = result.copy(isAcknowledged = true)
        }
    }

    internal suspend fun acknowledgePublishedModal(resultId: Long) {
        before(StubOperation.ACKNOWLEDGE_PUBLISHED_MODAL)
        stateMutex.withLock {
            val result = reviewResults[resultId]
                ?: throw NoSuchElementException("review result $resultId does not exist")
            check(result.decision == ReviewDecision.APPROVED) { "only approval has a published modal" }
            reviewResults[resultId] = result.copy(publishedModalAcknowledged = true)
        }
    }

    private suspend fun transition(
        operation: StubOperation,
        spotId: Long,
        allowed: Set<MySpotStatus>,
        destination: MySpotStatus,
    ): MySpotTransitionResult {
        before(operation, spotId)
        return stateMutex.withLock {
            val record = ownedRecord(spotId)
            requireStatus(record, allowed)
            record.status = destination
            record.updatedAt = now()
            record.toTransitionResult()
        }
    }

    private suspend fun before(operation: StubOperation, spotId: Long? = null) {
        while (true) {
            val response = synchronized(responseLock) {
                responses[operation]?.let { queue ->
                    if (queue.isEmpty()) null else queue.removeFirst()
                }
            } ?: return
            when (response) {
                StubResponse.Success -> return
                is StubResponse.Delay -> delay(response.millis)
                is StubResponse.Failure -> throw StubServiceException(response.message)
                is StubResponse.ReviewRace -> {
                    val id = requireNotNull(spotId) { "review race requires a spot id" }
                    val result = completeReview(id, response.decision, response.rejectionReason)
                    val latest = when (result.decision) {
                        ReviewDecision.APPROVED -> MySpotStatus.PUBLISHED
                        ReviewDecision.REJECTED -> MySpotStatus.REJECTED
                    }
                    throw MySpotTransitionConflictException(id, latest)
                }
            }
        }
    }

    private fun ownedRecord(spotId: Long): StubSpotRecord {
        val record = records[spotId]
            ?.takeUnless { it.isDeleted }
            ?: throw NoSuchElementException("spot $spotId does not exist")
        check(record.isOwnedByCurrentUser) { "spot $spotId is not owned by current user" }
        return record
    }

    private fun publicRecord(spotId: Long): StubSpotRecord {
        val record = records[spotId]
            ?.takeUnless { it.isDeleted }
            ?: throw NoSuchElementException("spot $spotId does not exist")
        check(record.status == MySpotStatus.PUBLISHED) { "spot $spotId is private" }
        return record
    }

    private fun requireStatus(record: StubSpotRecord, allowed: Set<MySpotStatus>) {
        check(record.status in allowed) {
            "spot ${record.id} is ${record.status}; expected one of $allowed"
        }
    }

    private fun resetState() {
        records.clear()
        StubSpotFixtures.records().forEach { records[it.id] = it }
        reviewResults.clear()
        nextSpotId = 42_000L
        nextReviewResultId = 1L
        clockTick = 0L
    }

    private fun now(): String = Instant.parse("2026-08-06T10:00:00Z")
        .plusSeconds(clockTick++)
        .toString()
}

private fun StubSpotRecord.toMySpot(coordinates: Coordinates?): MySpot = MySpot(
    id = id,
    name = name,
    theme = theme,
    imageUrl = imageUrl,
    latitude = latitude,
    longitude = longitude,
    distanceKm = coordinates?.let(::distanceFrom),
    createdAt = createdAt,
    status = status,
    bookmarkCount = bookmarkCount,
)

private fun StubSpotRecord.toMySpotDetail(): MySpotDetail = MySpotDetail(
    id = id,
    name = name,
    theme = theme,
    imageUrl = imageUrl,
    latitude = latitude,
    longitude = longitude,
    address = address,
    capturedDate = capturedDate,
    capturedTime = capturedTime,
    comment = comment,
    status = status,
    rejection = rejection,
    recommendationCount = recommendationCount,
    isRecommended = isRecommended,
    source = source,
    updatedAt = updatedAt,
)

private fun StubSpotRecord.toTransitionResult() = MySpotTransitionResult(
    spotId = id,
    status = status,
    updatedAt = updatedAt,
)

private fun StubSpotRecord.toRecommendationResult() = RecommendationResult(
    spotId = id,
    recommendationCount = recommendationCount,
    isRecommended = isRecommended,
)

private fun StubSpotRecord.toSpot(coordinates: Coordinates?) = Spot(
    id = id.toString(),
    name = name,
    theme = theme,
    latitude = latitude,
    longitude = longitude,
    imageUrl = imageUrl,
    address = address,
    distanceKm = coordinates?.let(::distanceFrom),
)

private fun StubSpotRecord.toSavedSpot(coordinates: Coordinates?): SavedSpot {
    val availability = when {
        isDeleted -> SavedSpotAvailability.DELETED
        source == com.pickflow.android.core.services.protocols.SpotSource.User &&
            status != MySpotStatus.PUBLISHED -> SavedSpotAvailability.AUTHOR_PRIVATE
        else -> SavedSpotAvailability.AVAILABLE
    }
    return SavedSpot(
        id = id,
        name = name,
        theme = theme,
        imageUrl = imageUrl,
        latitude = latitude,
        longitude = longitude,
        distanceKm = coordinates?.let(::distanceFrom),
        savedAt = createdAt,
        deleted = availability == SavedSpotAvailability.DELETED,
        availability = availability,
        isUserGenerated = source == com.pickflow.android.core.services.protocols.SpotSource.User,
    )
}

private fun StubSpotRecord.distanceFrom(coordinates: Coordinates): Double =
    hypot(latitude - coordinates.latitude, longitude - coordinates.longitude) * 111.0
