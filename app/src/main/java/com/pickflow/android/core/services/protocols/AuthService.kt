package com.pickflow.android.core.services.protocols

interface AuthService {
    suspend fun logout()
    suspend fun withdraw()
    suspend fun isLoggedIn(): Boolean
}
