package com.pickflow.android.core.network

import com.pickflow.android.core.network.api.RefreshApi
import com.pickflow.android.core.network.dto.auth.RefreshRequest
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import com.pickflow.android.core.services.protocols.TokenStore

/**
 * OkHttp Authenticator — 401 응답 시 RefreshApi.refresh 를 호출해 TokenStore를 갱신하고
 * 원요청에 새 Bearer 토큰을 붙여 단 1회 재시도한다.
 *
 * - 동시 401에 대비해 Mutex로 직렬화. 첫 요청이 토큰을 갱신한 뒤 후속 요청은 갱신된
 *   토큰을 그대로 사용.
 * - refresh 실패(예: 만료된 refresh token)인 경우 TokenStore.clear() 후 null 반환하여
 *   화면 측에서 LoadState.Failed로 흐르도록 한다.
 * - refresh 요청 자체에 의한 401은 재시도하지 않도록 Response.priorResponse 체인 길이로
 *   판단 (1회 시도 후 포기).
 * - RefreshApi 주입에 Provider를 쓰는 이유: NetworkModule에서 동일 SingletonComponent
 *   안의 Retrofit/RefreshApi가 OkHttpClient를 거꾸로 의존하지 않도록 lazy 해석.
 */
@Singleton
class TokenAuthenticator @Inject constructor(
    private val refreshApiProvider: Provider<RefreshApi>,
    private val tokenStore: TokenStore,
) : Authenticator {

    private val mutex = Mutex()

    override fun authenticate(route: Route?, response: Response): Request? {
        // 무한 루프 방지: 같은 요청에 이미 한 번 재시도했다면 포기
        if (priorResponseCount(response) >= 1) return null

        val originalAuth = response.request.header("Authorization")
        return runBlocking {
            mutex.withLock {
                val current = tokenStore.accessToken()
                val refresh = tokenStore.refreshToken() ?: return@withLock null

                // 다른 401이 이미 토큰을 갱신했다면 그 새 토큰을 사용해 재시도
                if (current != null && originalAuth != "Bearer $current") {
                    return@withLock response.request.newBuilder()
                        .header("Authorization", "Bearer $current")
                        .build()
                }

                val newTokens = try {
                    refreshApiProvider.get()
                        .refresh(RefreshRequest(refreshToken = refresh))
                        .unwrap()
                } catch (t: Throwable) {
                    tokenStore.clear()
                    return@withLock null
                }

                tokenStore.save(newTokens.accessToken, newTokens.refreshToken)
                response.request.newBuilder()
                    .header("Authorization", "Bearer ${newTokens.accessToken}")
                    .build()
            }
        }
    }

    private fun priorResponseCount(response: Response): Int {
        var count = 0
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}
