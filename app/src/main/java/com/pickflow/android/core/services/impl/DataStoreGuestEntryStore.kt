package com.pickflow.android.core.services.impl

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.pickflow.android.core.services.protocols.GuestEntryStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

private val Context.guestEntryDataStore: DataStore<Preferences> by preferencesDataStore(name = "guest_entry")
private val KEY_ENTERED = booleanPreferencesKey("entered")

/** 비회원 진입 이력을 DataStore Preferences 로 영구 저장 — 앱 재시작 후에도 유지. */
@Singleton
class DataStoreGuestEntryStore @Inject constructor(
    @ApplicationContext private val context: Context,
) : GuestEntryStore {

    override suspend fun hasEntered(): Boolean =
        context.guestEntryDataStore.data.first()[KEY_ENTERED] ?: false

    override suspend fun setEntered(entered: Boolean) {
        context.guestEntryDataStore.edit { it[KEY_ENTERED] = entered }
    }
}
