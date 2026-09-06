package com.pickflow.android.feature.archive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pickflow.android.core.services.protocols.NewFeatureGuide
import com.pickflow.android.core.services.protocols.NewFeatureKeys
import com.pickflow.android.core.services.protocols.SpotOpenGuideStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * "스팟 공개 OPEN!" 안내 바텀시트 — 보관함 > 나만의 스팟 탭.
 *
 * 노출 = `원격 활성(Remote Config) && 아직 안 봤음(계정별) && 로그인됨`.
 *
 * **로그인 게이팅이 여기서는 맞다.** 안내 대상이 "내 스팟" 이라 비로그인에는 띄울 것이 없다.
 * (V2 안내는 정반대다 — 게스트도 받아야 한다. PV-79 §6)
 * 게이트는 [SpotOpenGuideStore.hasSeen] 이 비로그인에 null 을 주는 것으로 걸린다 —
 * `== false` 한 번에 "로그인했고 아직 안 봤다" 가 된다.
 */
@HiltViewModel
class SpotOpenGuideViewModel @Inject constructor(
    private val guide: NewFeatureGuide,
    private val store: SpotOpenGuideStore,
) : ViewModel() {

    /** "봤음" 은 suspend 라 StateFlow 로 못 흘린다. 바뀔 때마다 여기를 튕겨 재평가시킨다. */
    private val revision = MutableStateFlow(0)

    val visible: StateFlow<Boolean> = combine(guide.config, revision) { _, _ -> evaluate() }
        // 초깃값은 false — fetch 전에는 꺼져 있는 게 안전한 기본값이다.
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /**
     * 화면 진입 시 호출. 원격 설정을 새로 받고 재평가한다.
     *
     * 재평가가 필요한 이유: 보관함이 로그인 진입점이라 **같은 화면에 머문 채 로그인**할 수 있다.
     * 그때 계정이 생기므로 다시 판정해야 한다.
     */
    fun onAppear() {
        revision.value++
        viewModelScope.launch { guide.refresh() }
    }

    fun confirm() {
        viewModelScope.launch {
            store.markSeen()
            revision.value++
        }
    }

    private suspend fun evaluate(): Boolean =
        store.hasSeen() == false && guide.isActive(NewFeatureKeys.SPOT_OPEN_GUIDE)
}
