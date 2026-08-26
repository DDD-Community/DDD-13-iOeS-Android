package com.pickflow.android.core.network

import com.pickflow.android.core.services.protocols.ApiEnvironment
import com.pickflow.android.core.services.protocols.DevSettings
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Dev Mode 에서 고른 API 환경으로 요청 host 를 바꿔치기한다.
 *
 * Retrofit 의 baseUrl 은 빌드 시점에 고정되므로, 런타임 전환은 여기서 host 만 교체해 처리한다.
 * dev/prod 는 path 규칙이 같고 host 만 다르며, Pickflow API 가 아닌 요청(카카오 등)은 건드리지 않는다.
 */
@Singleton
class ApiEnvironmentInterceptor @Inject constructor(
    private val devSettings: DevSettings,
) : Interceptor {

    private val hosts: Map<ApiEnvironment, String> =
        ApiEnvironment.entries.associateWith { it.baseUrl.toHttpUrl().host }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val host = request.url.host
        val targetHost = hosts.getValue(devSettings.apiEnvironment.value)
        if (host == targetHost || host !in hosts.values) return chain.proceed(request)

        val rewritten = request.url.newBuilder().host(targetHost).build()
        return chain.proceed(request.newBuilder().url(rewritten).build())
    }
}
