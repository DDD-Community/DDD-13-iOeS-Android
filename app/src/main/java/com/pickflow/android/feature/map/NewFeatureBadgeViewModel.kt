package com.pickflow.android.feature.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pickflow.android.core.services.protocols.NewFeatureGuide
import com.pickflow.android.core.services.protocols.NewFeatureKeys
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 무드 필터 칩의 신규 dot 노출 여부 — Remote Config `home_new_badge` 가 정한다.
 * 어느 무드에 붙는지는 여전히 클라이언트가 안다([MoodFilter.isNew]). 여기서 정하는 건 켜고 끄기뿐이다.
 *
 * 지도와 리스트가 같은 `MoodFilterRow` 를 쓰므로 두 화면이 이 ViewModel 을 각자 하나씩 갖는다
 * (판정 근거인 [NewFeatureGuide] 가 @Singleton 이라 결과는 같다).
 */
@HiltViewModel
class NewFeatureBadgeViewModel @Inject constructor(
    private val guide: NewFeatureGuide,
) : ViewModel() {

    val newBadgeVisible: StateFlow<Boolean> = guide.config
        .map { active() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, active())

    init {
        viewModelScope.launch { guide.refresh() }
    }

    private fun active() = guide.isActive(NewFeatureKeys.HOME_NEW_BADGE)
}
