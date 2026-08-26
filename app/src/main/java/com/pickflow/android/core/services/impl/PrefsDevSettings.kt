package com.pickflow.android.core.services.impl

import android.content.Context
import androidx.core.content.edit
import com.pickflow.android.BuildConfig
import com.pickflow.android.core.services.protocols.ApiEnvironment
import com.pickflow.android.core.services.protocols.DevSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

private const val PREFS = "dev_settings"
private const val KEY_ENVIRONMENT = "api_environment"
private const val KEY_BADGE = "badge_enabled"
private const val KEY_TOUCH = "touch_indicator_enabled"

/**
 * DataStore 대신 SharedPreferences 를 쓰는 이유: OkHttp 인터셉터가 요청 스레드에서
 * 현재 환경을 동기로 읽어야 하고, 앱 시작 첫 요청부터 저장된 값이 적용돼야 한다.
 *
 * 릴리스 빌드에서는 저장값을 읽지도 쓰지도 않는다 — Dev Mode 가 없는 것처럼 항상 기본값으로 굳는다.
 * 화면으로 들어갈 길이 막혀 있어도(HomeScreen 의 BuildConfig.DEBUG 가드) 여기서 한 번 더 잠근다.
 */
@Singleton
class PrefsDevSettings @Inject constructor(
    @ApplicationContext context: Context,
) : DevSettings {

    private val prefs = context
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .takeIf { BuildConfig.DEBUG }

    private val _apiEnvironment = MutableStateFlow(
        prefs?.getString(KEY_ENVIRONMENT, null)
            ?.let { saved -> ApiEnvironment.entries.firstOrNull { it.name == saved } }
            ?: ApiEnvironment.DEFAULT,
    )
    override val apiEnvironment: StateFlow<ApiEnvironment> = _apiEnvironment.asStateFlow()

    private val _badgeEnabled = MutableStateFlow(prefs?.getBoolean(KEY_BADGE, false) ?: false)
    override val badgeEnabled: StateFlow<Boolean> = _badgeEnabled.asStateFlow()

    private val _touchIndicatorEnabled = MutableStateFlow(prefs?.getBoolean(KEY_TOUCH, false) ?: false)
    override val touchIndicatorEnabled: StateFlow<Boolean> = _touchIndicatorEnabled.asStateFlow()

    override fun setApiEnvironment(environment: ApiEnvironment) {
        val prefs = prefs ?: return
        prefs.edit { putString(KEY_ENVIRONMENT, environment.name) }
        _apiEnvironment.value = environment
    }

    override fun setBadgeEnabled(enabled: Boolean) {
        val prefs = prefs ?: return
        prefs.edit { putBoolean(KEY_BADGE, enabled) }
        _badgeEnabled.value = enabled
    }

    override fun setTouchIndicatorEnabled(enabled: Boolean) {
        val prefs = prefs ?: return
        prefs.edit { putBoolean(KEY_TOUCH, enabled) }
        _touchIndicatorEnabled.value = enabled
    }
}
