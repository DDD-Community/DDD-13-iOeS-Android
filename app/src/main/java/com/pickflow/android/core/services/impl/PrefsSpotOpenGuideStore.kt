package com.pickflow.android.core.services.impl

import android.content.Context
import androidx.core.content.edit
import com.pickflow.android.BuildConfig
import com.pickflow.android.core.services.protocols.SpotOpenGuideStore
import com.pickflow.android.core.services.protocols.TokenStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private const val PREFS = "spot_open_guide"

/**
 * 계정 + 앱 버전 스코프. 버전이 오르면 다시 한 번 안내한다
 * (V2 안내의 `v2UpdateModalSeen.<앱버전>` 과 같은 성질에 계정을 한 겹 더 얹었다).
 */
private fun seenKey(userId: String) = "spotOpenGuideSeen.$userId.${BuildConfig.VERSION_NAME}"

/**
 * 로그아웃해도 지우지 않는다 — 그게 "계정별" 의 뜻이다. 같은 계정으로 다시 로그인하면
 * 이미 본 사람은 다시 보지 않고, 다른 계정으로 들어오면 그 계정 기준으로 새로 판정한다.
 */
@Singleton
class PrefsSpotOpenGuideStore @Inject constructor(
    @ApplicationContext context: Context,
    private val tokenStore: TokenStore,
) : SpotOpenGuideStore {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    override suspend fun hasSeen(): Boolean? {
        val userId = tokenStore.userId() ?: return null
        return prefs.getBoolean(seenKey(userId), false)
    }

    override suspend fun markSeen() {
        val userId = tokenStore.userId() ?: return
        prefs.edit { putBoolean(seenKey(userId), true) }
    }
}
