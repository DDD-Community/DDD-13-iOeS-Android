package com.pickflow.android.core.services.impl

import com.pickflow.android.core.services.protocols.MoodFilterStore
import com.pickflow.android.core.services.protocols.SpotTheme
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * [MoodFilterStore] 인메모리 구현 — 지도·리스트가 같은 인스턴스를 본다.
 *
 * `@Singleton` 이 이 서비스의 본질이다. 프로세스에 하나여야 두 ViewModel 이
 * 같은 선택을 공유한다. 앱을 껐다 켜면 초기화되는데, 필터는 세션 단위 상태라
 * 영속화하지 않는 편이 자연스럽다(진입 시 전체 목록).
 */
@Singleton
class InMemoryMoodFilterStore @Inject constructor() : MoodFilterStore {

    private val _selected = MutableStateFlow<Set<SpotTheme>>(emptySet())
    override val selected: StateFlow<Set<SpotTheme>> = _selected.asStateFlow()

    override fun toggle(theme: SpotTheme) {
        _selected.value = _selected.value.toMutableSet().apply {
            if (!add(theme)) remove(theme)
        }
    }

    override fun clear() {
        _selected.value = emptySet()
    }
}
