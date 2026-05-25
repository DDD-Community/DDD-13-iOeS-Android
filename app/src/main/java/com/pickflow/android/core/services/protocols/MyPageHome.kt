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

/**
 * PATCH /v1/users/me 응답 모델. 닉네임/프로필 이미지 부분 수정 결과.
 */
data class UpdateProfileResult(
    val displayName: String,
    val profileImageUrl: String?,
)

/**
 * Multipart 업로드 페이로드. Service 인터페이스가 OkHttp 타입에 묶이지 않도록 추상화.
 * - bytes: 실제 이미지 데이터
 * - mimeType: image/jpeg 등
 * - filename: 서버 측 로깅/검증용 (확장자 포함)
 */
data class ImagePayload(
    val bytes: ByteArray,
    val mimeType: String,
    val filename: String,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ImagePayload) return false
        return mimeType == other.mimeType && filename == other.filename && bytes.contentEquals(other.bytes)
    }

    override fun hashCode(): Int {
        var result = bytes.contentHashCode()
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + filename.hashCode()
        return result
    }
}
