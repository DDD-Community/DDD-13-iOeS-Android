package com.pickflow.android.core.services.impl

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import com.pickflow.android.BuildConfig
import com.pickflow.android.core.services.protocols.NewFeature
import com.pickflow.android.core.services.protocols.NewFeatureConfig
import com.pickflow.android.core.services.protocols.NewFeatureConfigProvider
import com.pickflow.android.core.services.protocols.NewFeatureGuide
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.days

private const val TAG = "NewFeatureGuide"
private const val PREFS = "new_feature_guide"

/**
 * 캐시 키에 앱 버전을 넣어 **버전이 올라가면 캐시가 저절로 비워진다**
 * (iOS `newFeatureGuide.v2.remoteConfig.<앱버전>` 과 같은 성질).
 */
private val KEY_CONFIG = "newFeatureGuide.v2.remoteConfig.${BuildConfig.VERSION_NAME}"

/**
 * 유저 기준 판정의 최초 판정 시각. 버전 스코프가 아니다 — "이 기기가 처음 본 뒤 N일" 이라는
 * 규칙에서 앱 업데이트는 아무 의미가 없어서다(계약에 명시된 항목이 아니라 판단한 부분).
 */
private fun firstSeenKey(key: String) = "newFeatureGuide.firstSeenAt.$key"

/**
 * Remote Config 판정 + 로컬 캐시.
 *
 * DataStore 가 아니라 SharedPreferences 인 이유는 [PrefsV2NoticeStore] 와 같다 —
 * 화면이 뜨기 전에 동기로 마지막 캐시를 읽어야 첫 프레임부터 판정이 맞는다.
 */
@Singleton
class PrefsNewFeatureGuide @Inject constructor(
    @ApplicationContext context: Context,
    private val provider: NewFeatureConfigProvider,
) : NewFeatureGuide {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    private val _config = MutableStateFlow(
        prefs.getString(KEY_CONFIG, null)?.let { cached ->
            runCatching { json.decodeFromString<NewFeatureConfig>(cached) }.getOrNull()
        },
    )
    override val config: StateFlow<NewFeatureConfig?> = _config.asStateFlow()

    override suspend fun refresh() {
        val fetched = runCatching { provider.fetchFeatureConfig() }
            .onFailure {
                if (BuildConfig.DEBUG) Log.w(TAG, "fetch 실패 — 마지막 캐시로 판정한다", it)
            }
            .getOrNull()
        // fetch 가 깨져도 이미 activate 된 값이 있으면 그걸 쓰고, 그것도 없으면 캐시를 그대로 둔다.
            ?: provider.activatedFeatureConfig()
            ?: return

        prefs.edit { putString(KEY_CONFIG, json.encodeToString(fetched)) }
        _config.value = fetched
    }

    override fun isActive(key: String, now: Long): Boolean {
        val feature = _config.value?.features?.firstOrNull { it.key == key }
        val active = feature != null && feature.isActiveAt(now)
        if (BuildConfig.DEBUG) Log.d(TAG, "isActive($key)=$active feature=$feature")
        return active
    }

    private fun NewFeature.isActiveAt(now: Long): Boolean {
        if (startAt != null) {
            // 런칭 기준 — 전 유저 공통 스케줄. endAt 이 있으면 durationDays 는 무시된다.
            val end = endAt
                ?: durationDays?.let { startAt + it.days.inWholeMilliseconds }
                // 끝이 없으면 "무기한" 이 아니라 아예 꺼짐. 영구 노출은 실수로만 만들어진다.
                ?: return false
            return now in startAt until end
        }
        // 유저(기기) 기준 — 최초 "판정" 시각부터 durationDays 동안.
        val duration = durationDays?.days?.inWholeMilliseconds ?: return false
        return now - firstSeenAt(key, now) < duration
    }

    /** 없으면 [now] 를 심고 그대로 돌려준다 — 최초 판정 시각이 곧 기준점이다. */
    private fun firstSeenAt(key: String, now: Long): Long {
        val saved = prefs.getLong(firstSeenKey(key), 0L)
        if (saved != 0L) return saved
        prefs.edit { putLong(firstSeenKey(key), now) }
        return now
    }
}
