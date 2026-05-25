package com.pickflow.android.core.network

import com.pickflow.android.core.services.protocols.TokenStore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenStore: TokenStore,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        if (original.header(AUTH_HEADER) != null) return chain.proceed(original)
        val token = runBlocking { tokenStore.accessToken() } ?: return chain.proceed(original)
        val authed = original.newBuilder()
            .header(AUTH_HEADER, "Bearer $token")
            .build()
        return chain.proceed(authed)
    }

    companion object {
        const val AUTH_HEADER = "Authorization"
    }
}
