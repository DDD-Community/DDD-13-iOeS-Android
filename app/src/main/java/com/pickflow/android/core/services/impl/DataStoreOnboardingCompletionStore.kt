package com.pickflow.android.core.services.impl

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.pickflow.android.core.services.protocols.OnboardingCompletionStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

private val Context.onboardingDataStore: DataStore<Preferences> by preferencesDataStore(name = "onboarding")
private val KEY_COMPLETED = booleanPreferencesKey("completed")

/**
 * iOS UserDefaults("onboarding_completed") 1:1 대응 — Jetpack DataStore Preferences로 영구 저장.
 * 앱 재시작 후에도 온보딩 완료 여부 유지.
 */
@Singleton
class DataStoreOnboardingCompletionStore @Inject constructor(
    @ApplicationContext private val context: Context,
) : OnboardingCompletionStore {

    override suspend fun isCompleted(): Boolean =
        context.onboardingDataStore.data.first()[KEY_COMPLETED] ?: false

    override suspend fun markCompleted() {
        context.onboardingDataStore.edit { it[KEY_COMPLETED] = true }
    }
}
