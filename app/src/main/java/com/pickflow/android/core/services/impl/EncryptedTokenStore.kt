package com.pickflow.android.core.services.impl

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.pickflow.android.core.services.protocols.TokenStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * EncryptedSharedPreferences 기반 [TokenStore] — 앱 재시작 시에도 토큰을 유지.
 *
 * - AES256_GCM 으로 값/키 모두 암호화.
 * - 모든 I/O 는 [Dispatchers.IO] 로 오프로드 (메인 스레드 ANR 방지).
 * - `MasterKey` 생성 비용이 있어 [prefs] 는 lazy 초기화 후 [mutex] 로 단일화.
 *
 * 마이그레이션: 기존 `InMemoryTokenStore` 와 동일 인터페이스라 DI 바인딩 교체만으로 적용.
 * 기 로그인 세션은 휘발됐으므로 사용자가 1회 재로그인 필요.
 */
@Singleton
class EncryptedTokenStore @Inject constructor(
    @ApplicationContext private val context: Context,
) : TokenStore {

    private val mutex = Mutex()

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            PREF_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    override suspend fun save(accessToken: String, refreshToken: String?) {
        withContext(Dispatchers.IO) {
            mutex.withLock {
                prefs.edit().apply {
                    putString(KEY_ACCESS, accessToken)
                    if (refreshToken != null) putString(KEY_REFRESH, refreshToken) else remove(KEY_REFRESH)
                    apply()
                }
            }
        }
    }

    override suspend fun accessToken(): String? = withContext(Dispatchers.IO) {
        mutex.withLock { prefs.getString(KEY_ACCESS, null) }
    }

    override suspend fun refreshToken(): String? = withContext(Dispatchers.IO) {
        mutex.withLock { prefs.getString(KEY_REFRESH, null) }
    }

    override suspend fun clear() {
        withContext(Dispatchers.IO) {
            mutex.withLock {
                prefs.edit().clear().apply()
            }
        }
    }

    private companion object {
        const val PREF_NAME = "pickflow_token_store"
        const val KEY_ACCESS = "access_token"
        const val KEY_REFRESH = "refresh_token"
    }
}
