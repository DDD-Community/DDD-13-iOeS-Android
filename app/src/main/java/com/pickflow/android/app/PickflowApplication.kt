package com.pickflow.android.app

import android.app.Application
import android.util.Log
import com.kakao.sdk.common.KakaoSdk
import com.pickflow.android.BuildConfig
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class PickflowApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKakaoSdk()
    }

    private fun initKakaoSdk() {
        if (BuildConfig.KAKAO_NATIVE_APP_KEY.isBlank()) return
        // 키 해시 조회 등 일부 환경(테스트/Robolectric)에서 실패할 수 있어 앱 기동을 막지 않도록 방어.
        runCatching { KakaoSdk.init(this, BuildConfig.KAKAO_NATIVE_APP_KEY) }
            .onFailure { Log.w("PickflowApplication", "KakaoSdk init skipped: ${it.message}") }
    }
}
