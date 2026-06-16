package com.pickflow.android.core.services.protocols

/**
 * iOS `AppVersionPolicy` 1:1 — `GET /v1/app/config/android` 응답.
 * iOS 와 동일 필드. Android 는 `storeUrl` 이 Play Store URL.
 */
data class AppVersionPolicy(
    val minimumVersion: String,
    val latestVersion: String,
    val forceUpdate: Boolean,
    val storeUrl: String,
    val supportEmail: String? = null,
    /** iOS `termsPolicies: [TermsPolicy]?` 1:1 — 약관/개인정보 등 문서 N개. */
    val termsPolicies: List<TermsPolicy>? = null,
)

interface AppVersionService {
    suspend fun fetchAndroidVersionPolicy(): AppVersionPolicy
}
