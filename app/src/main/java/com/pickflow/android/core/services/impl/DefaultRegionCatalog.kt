package com.pickflow.android.core.services.impl

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.pickflow.android.core.network.api.RegionApi
import com.pickflow.android.core.services.protocols.Region
import com.pickflow.android.core.services.protocols.RegionCatalog
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

private val Context.regionDataStore: DataStore<Preferences> by preferencesDataStore(name = "regions")
private val KEY_REGIONS = stringPreferencesKey("active_regions")

/** 캐시 레코드 — [Region.center] 는 앱이 id 로 붙이므로 저장하지 않는다. */
@Serializable
private data class CachedRegion(val id: Long, val displayName: String)

private val CACHE_FORMAT = ListSerializer(CachedRegion.serializer())

/**
 * [RegionCatalog] 구현 — DataStore 에 지역 목록을 JSON 으로 캐시한다.
 *
 * 지역은 거의 바뀌지 않아, 콜드 스타트나 오프라인에서도 마지막으로 본 목록을 바로 띄운다.
 * 같은 이유로 서버 조회는 **프로세스당 한 번**이다 — 지도·리스트를 오갈 때마다 부르지 않는다.
 *
 * 노출 순서는 서버 응답 순서를 믿지 않고 [Region.id] **내림차순**으로 앱이 정규화한다(2 대전 → 1 서울).
 * 최근 오픈한 지역을 먼저 보여주는 기획이라, 서버가 오름차순으로 주더라도 앱이 뒤집는다.
 */
@Singleton
class DefaultRegionCatalog @Inject constructor(
    @ApplicationContext private val context: Context,
    private val regionApi: RegionApi,
) : RegionCatalog {

    override suspend fun cached(): List<Region> {
        val raw = context.regionDataStore.data.first()[KEY_REGIONS] ?: return emptyList()
        return runCatching {
            Json.decodeFromString(CACHE_FORMAT, raw).map { Region(it.id, it.displayName) }.sortedById()
        }.getOrDefault(emptyList())
    }

    // 지도·리스트 ViewModel 이 각자 refresh 를 부르므로 동시 진입을 막아야 중복 요청이 안 생긴다.
    private val refreshLock = Mutex()
    private var refreshed = false

    override suspend fun refresh(): List<Region>? = refreshLock.withLock {
        if (refreshed) return@withLock null

        val items = runCatching { regionApi.getActiveRegions() }
            .getOrNull()
            ?.takeIf { it.success }
            ?.data
            ?.regions
            ?: return@withLock null
        if (items.isEmpty()) return@withLock null

        val regions = items.map { Region(it.regionId, it.regionName) }.sortedById()
        val encoded = Json.encodeToString(CACHE_FORMAT, regions.map { CachedRegion(it.id, it.displayName) })
        context.regionDataStore.edit { prefs -> prefs[KEY_REGIONS] = encoded }
        refreshed = true
        regions
    }
}

/** 지역 노출 순서 — 서버·캐시 순서와 무관하게 항상 `regionId` 내림차순(대전 → 서울). */
private fun List<Region>.sortedById(): List<Region> = sortedByDescending { it.id }
