package com.pickflow.android.core.services.protocols

interface AppleAuthProvider {
    /**
     * Apple SDK가 발급한 identity token(RS256 JWT)을 반환한다.
     * 최초 로그인 시에는 user(이름/이메일)도 전달될 수 있다.
     */
    suspend fun login(): AppleAuthResult
}

data class AppleAuthResult(
    val identityToken: String,
    val user: AppleAuthUser? = null,
)

data class AppleAuthUser(
    val email: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
)
