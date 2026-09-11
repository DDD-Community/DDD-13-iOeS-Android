package com.pickflow.android.core.network.mapper

import com.pickflow.android.core.network.dto.myspot.CancelPublicationResponseDto
import com.pickflow.android.core.network.dto.myspot.CreateMySpotResponseDto
import com.pickflow.android.core.network.dto.myspot.MySpotItemDto
import com.pickflow.android.core.network.dto.myspot.MySpotListResponseDto
import com.pickflow.android.core.network.dto.myspot.OpenMySpotResponseDto
import com.pickflow.android.core.network.dto.myspot.UpdateMySpotResponseDto
import com.pickflow.android.core.network.dto.spot.RejectionInfoDto
import com.pickflow.android.core.network.dto.spot.SpotDetailResponseDto
import com.pickflow.android.core.services.protocols.CreateMySpotResult
import com.pickflow.android.core.services.protocols.MySpot
import com.pickflow.android.core.services.protocols.MySpotPage
import com.pickflow.android.core.services.protocols.MySpotDetail
import com.pickflow.android.core.services.protocols.MySpotStatus
import com.pickflow.android.core.services.protocols.MySpotTransitionResult
import com.pickflow.android.core.services.protocols.MySpotUnpublishResult
import com.pickflow.android.core.services.protocols.MySpotUpdateResult
import com.pickflow.android.core.services.protocols.RejectionReason
import com.pickflow.android.core.services.protocols.SpotRejection
import com.pickflow.android.core.services.protocols.SpotSource

fun MySpotItemDto.toMySpot(): MySpot = MySpot(
    id = spotId,
    name = name,
    theme = parseTheme(theme),
    imageUrl = imageUrl?.takeIf { it.isNotBlank() },
    latitude = latitude,
    longitude = longitude,
    distanceKm = distanceKm,
    createdAt = createdAt,
    status = parseMySpotStatus(status),
    bookmarkCount = bookmarkCount,
    likeCount = likeCount,
    wasPublished = wasPublished,
)

fun MySpotListResponseDto.toMySpotPage(): MySpotPage = MySpotPage(
    items = spots.map { it.toMySpot() },
    page = page,
    hasNext = hasNext,
)

/** 서버 상태 5종을 그대로 매핑한다. `DRAFT`·`RE_REVIEW_PENDING` 이 빠지면 상세 화면 액션이 어긋난다. */
internal fun parseMySpotStatus(value: String): MySpotStatus = when (value.uppercase()) {
    "DRAFT" -> MySpotStatus.DRAFT
    "RE_REVIEW_PENDING" -> MySpotStatus.RE_REVIEW_PENDING
    "PUBLISHED" -> MySpotStatus.PUBLISHED
    "REJECTED" -> MySpotStatus.REJECTED
    else -> MySpotStatus.PENDING
}

fun CreateMySpotResponseDto.toCreateMySpotResult(): CreateMySpotResult = CreateMySpotResult(
    spotId = spotId,
    status = parseMySpotStatus(status),
    imageUrl = imageUrl?.takeIf { it.isNotBlank() },
)

/**
 * 나만의 스팟 상세. 전용 엔드포인트가 없어 `GET /v1/spots/{spotId}` 응답을 그대로 쓴다
 * (2026-08-26 OpenAPI 실측으로 확정 — `docs/PV-41/09-api-mapping.md`).
 */
fun SpotDetailResponseDto.toMySpotDetail(): MySpotDetail = MySpotDetail(
    id = spotId,
    name = name,
    theme = parseTheme(theme),
    imageUrl = imageUrl?.takeIf { it.isNotBlank() },
    latitude = latitude,
    longitude = longitude,
    address = address,
    capturedDate = recordedDate,
    capturedTime = recordedTime,
    comment = comment,
    status = parseMySpotStatus(status),
    rejection = rejection?.toSpotRejection(),
    recommendationCount = likeCount,
    isRecommended = isLiked,
    isMySpot = isMySpot,
    source = if (isCurated) SpotSource.Curated(displayName = imageCredit.orEmpty().trim()) else SpotSource.User,
)

internal fun RejectionInfoDto.toSpotRejection(): SpotRejection = SpotRejection(
    reason = parseRejectionReason(reason),
    reasonLabel = reasonLabel,
    guideMessage = guideMessage?.takeIf { it.isNotBlank() },
    detail = detail?.takeIf { it.isNotBlank() },
    rejectedAt = rejectedAt,
)

internal fun parseRejectionReason(value: String): RejectionReason = when (value.uppercase()) {
    "DUPLICATE" -> RejectionReason.DUPLICATE
    "LOW_QUALITY" -> RejectionReason.LOW_QUALITY
    "LOCATION_MISMATCH" -> RejectionReason.LOCATION_MISMATCH
    "FILTER_MISMATCH" -> RejectionReason.FILTER_MISMATCH
    else -> RejectionReason.ETC
}

fun OpenMySpotResponseDto.toTransitionResult(): MySpotTransitionResult = MySpotTransitionResult(
    spotId = spotId,
    status = parseMySpotStatus(status),
)

fun CancelPublicationResponseDto.toUnpublishResult(): MySpotUnpublishResult = MySpotUnpublishResult(
    spotId = spotId,
    previousStatus = parseMySpotStatus(previousStatus),
    status = parseMySpotStatus(status),
)

fun UpdateMySpotResponseDto.toUpdateResult(): MySpotUpdateResult = MySpotUpdateResult(
    spotId = spotId,
    status = parseMySpotStatus(status),
    imageUrl = imageUrl?.takeIf { it.isNotBlank() },
)
