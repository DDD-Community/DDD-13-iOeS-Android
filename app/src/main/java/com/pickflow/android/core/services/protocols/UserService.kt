package com.pickflow.android.core.services.protocols

interface UserService {
    suspend fun fetchMyPage(): MyPageHome

    /**
     * 닉네임/프로필 이미지 부분 수정. 둘 다 null이면 변경 없음(서버 처리).
     */
    suspend fun updateProfile(
        nickname: String? = null,
        profileImage: ImagePayload? = null,
    ): UpdateProfileResult

    /**
     * 닉네임만 필요한 화면용 편의 메서드. 기본 구현은 fetchMyPage().nickname 위임.
     * 추후 화면이 fetchMyPage()로 전면 전환되면 제거 예정.
     */
    suspend fun fetchUserName(): String = fetchMyPage().nickname
}
