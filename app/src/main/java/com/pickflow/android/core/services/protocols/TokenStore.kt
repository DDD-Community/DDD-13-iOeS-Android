package com.pickflow.android.core.services.protocols

interface TokenStore {
    suspend fun save(accessToken: String, refreshToken: String?)
    suspend fun accessToken(): String?
    suspend fun refreshToken(): String?

    /**
     * 로그인 응답의 `userId`. **계정 단위 로컬 플래그를 가르는 용도**다
     * (예: [SpotOpenGuideStore] — 같은 기기를 두 사람이 써도 각자 한 번씩 봐야 한다).
     *
     * 토큰 갱신([save])은 이 값을 건드리지 않는다 — 갱신 응답에는 프로필이 없다.
     * [clear] 는 토큰과 함께 지운다. 계정별 플래그 자체는 다른 저장소에 남아 재로그인해도 유지된다.
     */
    suspend fun saveUserId(userId: String)
    suspend fun userId(): String?

    suspend fun clear()
}
