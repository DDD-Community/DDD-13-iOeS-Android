package com.pickflow.android.core.services.impl

import android.content.Context
import androidx.core.content.edit
import com.pickflow.android.core.services.protocols.V2NoticeStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

private const val PREFS = "v2_notice"
private const val KEY_SEEN = "seen"

/**
 * DataStore 가 아니라 SharedPreferences 인 이유: Dev Mode 스위치와 지도가 같은 값을 실시간으로
 * 주고받아야 해서 StateFlow 가 필요한데, 앱 시작 시점에 동기로 초깃값을 읽을 수 있는 쪽이 짧다.
 * (Dev Mode 전용이 아니므로 [PrefsDevSettings] 와 달리 릴리스에서도 그대로 동작한다.)
 */
@Singleton
class PrefsV2NoticeStore @Inject constructor(
    @ApplicationContext context: Context,
) : V2NoticeStore {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val _seen = MutableStateFlow(prefs.getBoolean(KEY_SEEN, false))
    override val seen: StateFlow<Boolean> = _seen.asStateFlow()

    override fun setSeen(seen: Boolean) {
        prefs.edit { putBoolean(KEY_SEEN, seen) }
        _seen.value = seen
    }
}
