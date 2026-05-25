package com.pickflow.android.core.services.protocols

interface AuthService {
    suspend fun logout()
    suspend fun withdraw()
    suspend fun isLoggedIn(): Boolean

    /**
     * 소셜 로그인 시 탈퇴 감지로 발급된 restoreToken으로 계정을 복구한다.
     * 복구 후 호출자는 소셜 로그인을 재시도해야 한다.
     * TODO(BE confirm): restoreToken 발급 시점/경로 — 로그인 응답 에러 페이로드인지 별도 endpoint인지.
     */
    suspend fun restore(restoreToken: String)
}
