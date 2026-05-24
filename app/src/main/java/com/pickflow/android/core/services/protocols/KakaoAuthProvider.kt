package com.pickflow.android.core.services.protocols

interface KakaoAuthProvider {
    suspend fun login(): KakaoAuthResult
}

data class KakaoAuthResult(
    val accessToken: String,
    val refreshToken: String?,
)
