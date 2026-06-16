package com.pickflow.android.core.services.impl

import android.net.Uri

/**
 * iOS `ASAuthorizationAppleIDProvider` 가 내부적으로 발행하는 Apple Sign In OAuth URL 을
 * Android Custom Tabs 흐름용으로 생성. Apple Developer 콘솔에 등록된 Service ID + redirect URI 와
 * response_type=`code id_token`, response_mode=`form_post` (또는 `fragment`), `state`/`nonce`
 * 무작위 값을 포함.
 *
 * 실제 Custom Tabs 호출 + redirect intent 수신 + identityToken 추출은 Apple Developer
 * 콘솔에 Service ID/Redirect URI 가 등록된 후 별도 RealAppleAuthProvider 가 처리.
 */
object AppleOAuthUrl {
    /** iOS `ASAuthorizationScope.fullName,email` 대응. */
    const val SCOPE_NAME_EMAIL = "name email"
    const val RESPONSE_TYPE_CODE_ID_TOKEN = "code id_token"
    const val RESPONSE_MODE_FRAGMENT = "fragment"

    /**
     * Apple OAuth authorize URL 빌드.
     * @param serviceId Apple Developer 콘솔의 Service ID (`com.pickflow.web` 형식). BuildConfig 주입.
     * @param redirectUri 콘솔에 등록한 동일 URI. Android 는 deeplink (`https://pickflow-api.us/auth/apple`) 사용.
     * @param state CSRF 방지용 무작위 토큰. callback 시 비교 후 폐기.
     * @param nonce id_token replay 방지용 무작위 토큰. id_token nonce 클레임과 비교.
     */
    fun build(
        serviceId: String,
        redirectUri: String,
        state: String,
        nonce: String,
    ): Uri = Uri.parse("https://appleid.apple.com/auth/authorize")
        .buildUpon()
        .appendQueryParameter("client_id", serviceId)
        .appendQueryParameter("redirect_uri", redirectUri)
        .appendQueryParameter("response_type", RESPONSE_TYPE_CODE_ID_TOKEN)
        .appendQueryParameter("response_mode", RESPONSE_MODE_FRAGMENT)
        .appendQueryParameter("scope", SCOPE_NAME_EMAIL)
        .appendQueryParameter("state", state)
        .appendQueryParameter("nonce", nonce)
        .build()
}
