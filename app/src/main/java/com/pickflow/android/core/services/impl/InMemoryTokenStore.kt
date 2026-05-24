package com.pickflow.android.core.services.impl

import com.pickflow.android.core.services.protocols.TokenStore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Singleton
class InMemoryTokenStore @Inject constructor() : TokenStore {
    private val mutex = Mutex()
    private var access: String? = null
    private var refresh: String? = null

    override suspend fun save(accessToken: String, refreshToken: String?) = mutex.withLock {
        access = accessToken
        refresh = refreshToken
    }

    override suspend fun accessToken(): String? = mutex.withLock { access }
    override suspend fun refreshToken(): String? = mutex.withLock { refresh }
    override suspend fun clear() = mutex.withLock {
        access = null
        refresh = null
    }
}
