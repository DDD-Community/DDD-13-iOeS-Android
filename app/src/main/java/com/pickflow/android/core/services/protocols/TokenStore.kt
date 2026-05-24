package com.pickflow.android.core.services.protocols

interface TokenStore {
    suspend fun save(accessToken: String, refreshToken: String?)
    suspend fun accessToken(): String?
    suspend fun refreshToken(): String?
    suspend fun clear()
}
