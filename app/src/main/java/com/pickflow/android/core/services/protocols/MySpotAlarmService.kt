package com.pickflow.android.core.services.protocols

/**
 * 본인이 등록한 스팟의 촬영조건(노을/윤슬 조건 충족 등) 알림 구독.
 * - 구독 이력이 없으면 enabled=false로 반환
 */
data class SpotAlarm(
    val spotId: Long,
    val enabled: Boolean,
)

interface MySpotAlarmService {
    suspend fun get(spotId: Long): SpotAlarm
    suspend fun update(spotId: Long, enabled: Boolean): SpotAlarm
}
