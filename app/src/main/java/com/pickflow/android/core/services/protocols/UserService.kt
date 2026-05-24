package com.pickflow.android.core.services.protocols

interface UserService {
    suspend fun fetchUserName(): String
}
