package com.pickflow.android.core.services.impl

import com.pickflow.android.core.services.protocols.Region
import com.pickflow.android.core.services.protocols.RegionStore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * [RegionStore] 인메모리 구현 — 지도·리스트가 같은 인스턴스를 본다.
 *
 * `@Singleton` 이 본질이다(프로세스 하나). 무드 필터와 마찬가지로 세션 단위 상태라
 * 영속화하지 않는다 — 앱을 다시 켜면 서울로 돌아간다.
 */
@Singleton
class InMemoryRegionStore @Inject constructor() : RegionStore {

    private val _selected = MutableStateFlow(Region.Seoul)
    override val selected: StateFlow<Region> = _selected.asStateFlow()

    override fun select(region: Region) {
        _selected.value = region
    }
}
