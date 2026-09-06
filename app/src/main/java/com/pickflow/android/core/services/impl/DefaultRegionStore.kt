package com.pickflow.android.core.services.impl

import com.pickflow.android.core.services.protocols.Region
import com.pickflow.android.core.services.protocols.RegionCatalog
import com.pickflow.android.core.services.protocols.RegionStore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * [RegionStore] 구현 — 지도·리스트가 같은 인스턴스를 본다.
 *
 * `@Singleton` 이 본질이다(프로세스 하나). 선택 상태는 무드 필터와 마찬가지로 세션 단위라
 * 영속화하지 않는다 — 앱을 다시 켜면 첫 지역으로 돌아간다. 목록만 [RegionCatalog] 가 캐시한다.
 */
@Singleton
class DefaultRegionStore @Inject constructor(
    private val regionCatalog: RegionCatalog,
) : RegionStore {

    private val _selected = MutableStateFlow(Region.Seoul)
    override val selected: StateFlow<Region> = _selected.asStateFlow()

    private val _available = MutableStateFlow(Region.FALLBACK)
    override val available: StateFlow<List<Region>> = _available.asStateFlow()

    override fun select(region: Region) {
        _selected.value = region
    }

    override suspend fun refreshAvailable() {
        // 캐시가 있으면 네트워크를 기다리지 않고 먼저 반영한다.
        apply(regionCatalog.cached())
        apply(regionCatalog.refresh().orEmpty())
    }

    private fun apply(regions: List<Region>) {
        // 빈 목록은 무시한다 — 지역이 하나도 없으면 스팟 조회 자체가 불가능하다.
        if (regions.isEmpty()) return
        _available.value = regions
        // 적용 중이던 지역이 목록에서 빠졌으면 죽은 regionId 로 스팟을 조회하게 된다.
        if (regions.none { it.id == _selected.value.id }) _selected.value = regions.first()
    }
}
