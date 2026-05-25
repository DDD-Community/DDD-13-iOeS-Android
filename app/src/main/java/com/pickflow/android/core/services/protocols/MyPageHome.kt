package com.pickflow.android.core.services.protocols

/**
 * GET /v1/users/me 도메인 모델 — 마이페이지 홈 탭 헤더 표시용.
 * - nickname: "닉네임#해시태그" 형식 (서버 displayName과 동일)
 * - profileImageUrl: 빈 문자열은 null로 정규화
 */
data class MyPageHome(
    val nickname: String,
    val profileImageUrl: String?,
    val savedSpotCount: Long,
    val recordedSpotCount: Long,
)
