package com.pickflow.android.core.services.impl

import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.pickflow.android.BuildConfig
import com.pickflow.android.core.services.protocols.NewFeatureConfig
import com.pickflow.android.core.services.protocols.NewFeatureConfigProvider
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val TAG = "NewFeatureGuide"

/** iOS 와 공유하는 Remote Config 파라미터 이름. 바꾸면 양쪽이 같이 깨진다. */
private const val PARAM_KEY = "new_feature_flags"

/**
 * DEBUG 는 0초 — QA 가 Console 에서 값을 바꾸고 앱만 다시 들어가면 바로 보인다.
 * Release 는 1시간(Firebase 권장 하한).
 */
private val FETCH_INTERVAL_SECONDS = if (BuildConfig.DEBUG) 0L else 3600L

/**
 * Firebase Remote Config 구현.
 *
 * **in-app default 를 등록하지 않는다.** 최초 fetch 전이나 파라미터가 없을 때 전부 꺼진 상태로
 * 남는 게 의도된 동작이다 — 안내 팝업이 실수로 전 유저에게 뜨는 쪽이 훨씬 나쁘다.
 */
@Singleton
class FirebaseNewFeatureConfigProvider @Inject constructor() : NewFeatureConfigProvider {

    // google-services.json 이 없는 워크트리(시크릿 미주입)에서 getInstance() 가 던진다.
    // 앱을 죽이지 말고 "설정 없음 = 전부 꺼짐" 으로 흘려보낸다.
    private val remoteConfig: FirebaseRemoteConfig? by lazy {
        runCatching {
            FirebaseRemoteConfig.getInstance().apply {
                setConfigSettingsAsync(
                    FirebaseRemoteConfigSettings.Builder()
                        .setMinimumFetchIntervalInSeconds(FETCH_INTERVAL_SECONDS)
                        .build(),
                )
            }
        }.onFailure {
            if (BuildConfig.DEBUG) Log.w(TAG, "Remote Config 를 쓸 수 없다 — 전부 꺼짐으로 동작한다", it)
        }.getOrNull()
    }

    override suspend fun fetchFeatureConfig(): NewFeatureConfig {
        val config = remoteConfig ?: error("Firebase Remote Config 초기화 실패")
        val activated = config.fetchAndActivate().await()
        val raw = config.getString(PARAM_KEY)
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "fetchAndActivate activated=$activated source=${config.info.lastFetchStatus}")
            Log.d(TAG, "$PARAM_KEY=$raw")
        }
        return parse(raw) ?: NewFeatureConfig()
    }

    override fun activatedFeatureConfig(): NewFeatureConfig? =
        remoteConfig?.getString(PARAM_KEY)?.let(::parse)

    private fun parse(raw: String): NewFeatureConfig? {
        if (raw.isBlank()) return null
        return runCatching { json.decodeFromString<NewFeatureConfig>(raw) }
            .onFailure { if (BuildConfig.DEBUG) Log.w(TAG, "$PARAM_KEY 파싱 실패: $raw", it) }
            .getOrNull()
    }

    private val json = Json { ignoreUnknownKeys = true }
}

/**
 * `Task.await()` 하나 때문에 kotlinx-coroutines-play-services 를 새로 물리지 않는다.
 * addOnCompleteListener 는 취소 통보가 없어 [suspendCancellableCoroutine] 만 쓴다.
 */
private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
    addOnCompleteListener { task ->
        val error = task.exception
        if (error != null) cont.resumeWithException(error) else cont.resume(task.result)
    }
}
