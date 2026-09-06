package com.pickflow.android.core.services.protocols

/**
 * 저장된 스팟 (GET /v1/users/me/saved-spots) 도메인 모델.
 * - id: 서버 Long
 * - distanceKm: 호출 시 좌표 미전달이면 null
 * - imageUrl: 빈 문자열은 null로 정규화
 * - savedAt: 북마크 저장 시각 (ISO-8601 date-time)
 * - deleted: 스팟이 운영 측에서 삭제됐는지 (UI에서 회색 처리 등에 활용)
 */
data class SavedSpot(
    val id: Long,
    val name: String,
    val theme: SpotTheme,
    val imageUrl: String?,
    val latitude: Double,
    val longitude: Double,
    val distanceKm: Double?,
    val savedAt: String,
    val deleted: Boolean,
    val availability: SavedSpotAvailability = if (deleted) {
        SavedSpotAvailability.DELETED
    } else {
        SavedSpotAvailability.AVAILABLE
    },
    val isUserGenerated: Boolean = false,
)

enum class SavedSpotAvailability { AVAILABLE, AUTHOR_PRIVATE, DELETED }

data class SavedSpotPage(
    val items: List<SavedSpot>,
    val page: Int,
    val hasNext: Boolean,
)
