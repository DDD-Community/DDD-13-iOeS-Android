package com.pickflow.android.core.services.impl

import android.content.Context
import androidx.core.content.edit
import com.pickflow.android.core.services.protocols.MySpot
import com.pickflow.android.core.services.protocols.MySpotService
import com.pickflow.android.core.services.protocols.MySpotStatus
import com.pickflow.android.core.services.protocols.ReviewDecision
import com.pickflow.android.core.services.protocols.ReviewResult
import com.pickflow.android.core.services.protocols.ReviewResultService
import com.pickflow.android.core.services.protocols.ReviewResultStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private const val PREFS = "review_result"
private const val KEY_STATE = "state"
private const val MAX_PAGES = 10

/**
 * 검수 결과 전용 엔드포인트가 서버에 없어서(`docs/PV-41/09-api-mapping.md` C 표),
 * 내 스팟 목록의 `status` 변화를 기기에 기억해 승인·반려를 감지한다.
 *
 * - 결과 id 는 스팟 id 다. 한 스팟에 살아있는 결과는 항상 하나뿐이다.
 * - 목록을 한 번은 읽어야 감지되므로 푸시처럼 즉시 뜨지는 않는다.
 * - 판정이 기기 로컬이라 다른 기기에서는 같은 결과가 다시 뜬다. 서버 계약이 생기면 통째로 교체한다.
 */
@Singleton
class StatusDiffReviewResultService @Inject constructor(
    private val mySpotService: MySpotService,
    @ApplicationContext context: Context,
) : ReviewResultService {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }
    private val mutex = Mutex()

    override suspend fun status(): ReviewResultStatus {
        val spots = fetchAllMySpots()
        val state = mutex.withLock {
            val next = read().applying(spots)
            write(next)
            next
        }
        return ReviewResultStatus(
            pendingRequestCount = spots.count { it.status in IN_REVIEW },
            unacknowledgedResults = state.results
                .map { (spotId, result) -> result.toDomain(spotId) }
                .filter { !it.isAcknowledged || it.decision == ReviewDecision.APPROVED && !it.publishedModalAcknowledged },
        )
    }

    override suspend fun acknowledge(resultId: Long) = update(resultId) { it.copy(acknowledged = true) }

    override suspend fun acknowledgePublishedModal(resultId: Long) =
        update(resultId) { it.copy(modalAcknowledged = true) }

    private suspend fun update(resultId: Long, transform: (StoredResult) -> StoredResult) {
        mutex.withLock {
            val state = read()
            val result = state.results[resultId.toString()] ?: return
            write(state.copy(results = state.results + (resultId.toString() to transform(result))))
        }
    }

    /** 결과가 목록 뒷페이지에 있을 수 있어 끝까지 읽는다. 페이지 수는 안전장치로 제한한다. */
    private suspend fun fetchAllMySpots(): List<MySpot> {
        val items = mutableListOf<MySpot>()
        var page = 0
        while (page < MAX_PAGES) {
            val result = mySpotService.list(page = page)
            items += result.items
            if (!result.hasNext) break
            page++
        }
        return items
    }

    private fun read(): ReviewState =
        prefs.getString(KEY_STATE, null)
            ?.let { runCatching { json.decodeFromString<ReviewState>(it) }.getOrNull() }
            ?: ReviewState()

    private fun write(state: ReviewState) {
        prefs.edit { putString(KEY_STATE, json.encodeToString(ReviewState.serializer(), state)) }
    }

    private companion object {
        val IN_REVIEW = setOf(MySpotStatus.PENDING, MySpotStatus.RE_REVIEW_PENDING)
    }
}

@Serializable
internal data class ReviewState(
    /** 마지막으로 본 스팟 상태. 이게 있어야 "검수중 → 공개" 전이를 결과로 승격할 수 있다. */
    val lastStatus: Map<String, String> = emptyMap(),
    val results: Map<String, StoredResult> = emptyMap(),
)

@Serializable
internal data class StoredResult(
    val decision: String,
    val occurredAt: String,
    val acknowledged: Boolean = false,
    val modalAcknowledged: Boolean = false,
)

/**
 * 직전 상태와 대조해 새 결과를 적립한다.
 * 처음 보는 스팟은 상태만 기록한다 — 설치 직후 과거 결과가 쏟아지지 않게 한다.
 */
internal fun ReviewState.applying(spots: List<MySpot>): ReviewState {
    val ids = spots.mapTo(mutableSetOf()) { it.id.toString() }
    val results = results.filterKeys { it in ids }.toMutableMap()
    val now = Instant.now().toString()

    spots.forEach { spot ->
        val key = spot.id.toString()
        val decision = decisionFor(lastStatus[key], spot.status)
        if (decision != null) {
            results[key] = StoredResult(decision = decision.name, occurredAt = now)
        }
    }
    return ReviewState(
        lastStatus = spots.associate { it.id.toString() to it.status.name },
        results = results,
    )
}

private fun decisionFor(previous: String?, current: MySpotStatus): ReviewDecision? {
    val wasInReview = previous == MySpotStatus.PENDING.name ||
        previous == MySpotStatus.RE_REVIEW_PENDING.name
    if (!wasInReview) return null
    return when (current) {
        MySpotStatus.PUBLISHED -> ReviewDecision.APPROVED
        MySpotStatus.REJECTED -> ReviewDecision.REJECTED
        else -> null
    }
}

internal fun StoredResult.toDomain(spotId: String): ReviewResult = ReviewResult(
    resultId = spotId.toLong(),
    spotId = spotId.toLong(),
    decision = ReviewDecision.valueOf(decision),
    occurredAt = occurredAt,
    isAcknowledged = acknowledged,
    publishedModalAcknowledged = modalAcknowledged,
)
