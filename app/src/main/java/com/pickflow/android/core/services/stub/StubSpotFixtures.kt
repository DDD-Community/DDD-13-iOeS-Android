package com.pickflow.android.core.services.stub

import com.pickflow.android.core.services.protocols.MySpotStatus
import com.pickflow.android.core.services.protocols.RejectionReason
import com.pickflow.android.core.services.protocols.SpotRejection
import com.pickflow.android.core.services.protocols.SpotSource
import com.pickflow.android.core.services.protocols.SpotTheme

object StubSpotFixtures {
    const val DRAFT_SPOT_ID = 41_001L
    const val PENDING_SPOT_ID = 41_002L
    const val RE_REVIEW_PENDING_SPOT_ID = 41_003L
    const val REJECTED_SPOT_ID = 41_004L
    const val PUBLISHED_USER_SPOT_ID = 41_005L
    const val CURATED_SPOT_ID = 41_006L
    const val DELETED_SAVED_SPOT_ID = 41_007L

    internal fun records(): List<StubSpotRecord> = listOf(
        record(DRAFT_SPOT_ID, "나만 보는 노을", MySpotStatus.DRAFT),
        record(PENDING_SPOT_ID, "검수 중인 노을", MySpotStatus.PENDING, latitude = 37.51),
        record(
            RE_REVIEW_PENDING_SPOT_ID,
            "재검수 중인 윤슬",
            MySpotStatus.RE_REVIEW_PENDING,
            theme = SpotTheme.YUNSEUL,
            latitude = 37.52,
        ),
        record(
            REJECTED_SPOT_ID,
            "보완이 필요한 노을",
            MySpotStatus.REJECTED,
            latitude = 37.53,
            rejection = StubRejections.of(
                RejectionReason.LOW_QUALITY,
                guideMessage = "사진에서 스팟을 확인하기 어려워요",
            ),
        ),
        record(
            PUBLISHED_USER_SPOT_ID,
            "공개된 유저 노을",
            MySpotStatus.PUBLISHED,
            latitude = 37.54,
            recommendationCount = 27,
            bookmarked = true,
        ),
        StubSpotRecord(
            id = CURATED_SPOT_ID,
            name = "한국관광공사 노을 명소",
            theme = SpotTheme.SUNSET,
            imageUrl = "stub://images/curated.jpg",
            latitude = 37.55,
            longitude = 127.05,
            address = "서울특별시 중구",
            capturedDate = "2026-08-01",
            capturedTime = "19:30",
            comment = "운영 큐레이션 스팟",
            status = MySpotStatus.PUBLISHED,
            rejection = null,
            recommendationCount = 105,
            isRecommended = false,
            source = SpotSource.Curated("한국관광공사"),
            isOwnedByCurrentUser = false,
            bookmarked = true,
            bookmarkCount = 13,
            createdAt = "2026-08-01T10:00:00Z",
            updatedAt = "2026-08-01T10:00:00Z",
        ),
        StubSpotRecord(
            id = DELETED_SAVED_SPOT_ID,
            name = "삭제된 큐레이션 스팟",
            theme = SpotTheme.YUNSEUL,
            imageUrl = null,
            latitude = 37.56,
            longitude = 127.06,
            address = "서울특별시 용산구",
            capturedDate = "2026-07-01",
            capturedTime = "19:00",
            comment = "운영 삭제 fixture",
            status = MySpotStatus.PUBLISHED,
            rejection = null,
            recommendationCount = 0,
            isRecommended = false,
            source = SpotSource.Curated("Pickflow 운영자"),
            isOwnedByCurrentUser = false,
            bookmarked = true,
            bookmarkCount = 1,
            isDeleted = true,
            createdAt = "2026-07-01T10:00:00Z",
            updatedAt = "2026-08-01T10:00:00Z",
        ),
    )

    private fun record(
        id: Long,
        name: String,
        status: MySpotStatus,
        theme: SpotTheme = SpotTheme.SUNSET,
        latitude: Double = 37.50,
        rejection: SpotRejection? = null,
        recommendationCount: Long = 0,
        bookmarked: Boolean = false,
    ) = StubSpotRecord(
        id = id,
        name = name,
        theme = theme,
        imageUrl = "stub://images/$id.jpg",
        latitude = latitude,
        longitude = 127.0 + (id % 100) / 1000.0,
        address = "서울특별시 테스트구 $id",
        capturedDate = "2026-08-01",
        capturedTime = "19:20",
        comment = "PV-41 deterministic fixture",
        status = status,
        rejection = rejection,
        recommendationCount = recommendationCount,
        isRecommended = false,
        source = SpotSource.User,
        isOwnedByCurrentUser = true,
        bookmarked = bookmarked,
        bookmarkCount = if (bookmarked) 1 else 0,
        createdAt = "2026-08-01T10:00:00Z",
        updatedAt = "2026-08-01T10:00:00Z",
    )
}

internal data class StubSpotRecord(
    val id: Long,
    var name: String,
    var theme: SpotTheme,
    var imageUrl: String?,
    var latitude: Double,
    var longitude: Double,
    var address: String,
    var capturedDate: String,
    var capturedTime: String,
    var comment: String,
    var status: MySpotStatus,
    var rejection: SpotRejection?,
    var recommendationCount: Long,
    var isRecommended: Boolean,
    val source: SpotSource,
    val isOwnedByCurrentUser: Boolean,
    var bookmarked: Boolean,
    var bookmarkCount: Long,
    var isDeleted: Boolean = false,
    val createdAt: String,
    var updatedAt: String,
)

/**
 * 반려 표시 문구는 프로덕션에서 서버가 `reasonLabel` / `guideMessage` 로 내려준다.
 * 스텁은 화면 검증용 고정 문구만 제공한다.
 */
internal object StubRejections {
    fun of(
        reason: RejectionReason,
        guideMessage: String? = null,
        detail: String? = null,
        rejectedAt: String = "2026-08-01T10:00:00Z",
    ): SpotRejection = SpotRejection(
        reason = reason,
        reasonLabel = label(reason),
        guideMessage = guideMessage ?: guide(reason),
        detail = detail,
        rejectedAt = rejectedAt,
    )

    fun label(reason: RejectionReason): String = when (reason) {
        RejectionReason.DUPLICATE -> "중복 스팟"
        RejectionReason.LOW_QUALITY -> "사진 상태 불량"
        RejectionReason.LOCATION_MISMATCH -> "위치 불일치"
        RejectionReason.FILTER_MISMATCH -> "테마 불일치"
        RejectionReason.ETC -> "기타"
    }

    private fun guide(reason: RejectionReason): String = when (reason) {
        RejectionReason.DUPLICATE -> "이미 등록된 스팟이 있어요"
        RejectionReason.LOW_QUALITY -> "사진에서 스팟을 확인하기 어려워요"
        RejectionReason.LOCATION_MISMATCH -> "사진과 위치가 일치하지 않아요"
        RejectionReason.FILTER_MISMATCH -> "선택한 테마와 사진이 맞지 않아요"
        RejectionReason.ETC -> "등록 정보를 다시 확인해주세요"
    }
}
