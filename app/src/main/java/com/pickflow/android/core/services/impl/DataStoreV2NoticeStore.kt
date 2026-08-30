package com.pickflow.android.core.services.impl

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.pickflow.android.core.services.protocols.V2NoticeStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

private val Context.v2NoticeDataStore: DataStore<Preferences> by preferencesDataStore(name = "v2_notice")
private val KEY_SEEN = booleanPreferencesKey("seen")

@Singleton
class DataStoreV2NoticeStore @Inject constructor(
    @ApplicationContext private val context: Context,
) : V2NoticeStore {

    override suspend fun isSeen(): Boolean =
        context.v2NoticeDataStore.data.first()[KEY_SEEN] ?: false

    override suspend fun markSeen() {
        context.v2NoticeDataStore.edit { it[KEY_SEEN] = true }
    }
}
