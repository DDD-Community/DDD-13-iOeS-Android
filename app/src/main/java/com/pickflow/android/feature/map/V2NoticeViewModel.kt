package com.pickflow.android.feature.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pickflow.android.BuildConfig
import com.pickflow.android.core.services.protocols.NewFeatureGuide
import com.pickflow.android.core.services.protocols.NewFeatureKeys
import com.pickflow.android.core.services.protocols.V2NoticeStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * V2 업데이트 안내 팝업 — **노출 여부는 Remote Config 가, "봤는지" 는 기기가 기억한다.**
 * "확인했어요" 를 누르면 플래그를 심어 다시 뜨지 않는다(Dev Mode 스위치로 되돌릴 수 있다).
 *
 * 로그인 여부는 보지 않는다. 안내하는 햇살·야경 필터가 게스트도 쓰는 기능이라서다 —
 * iOS 는 이 평가를 로그인 분기 안에 넣어 게스트가 모달을 영영 못 받는 버그가 났다(PV-79 §6).
 */
@HiltViewModel
class V2NoticeViewModel @Inject constructor(
    private val store: V2NoticeStore,
    private val guide: NewFeatureGuide,
) : ViewModel() {

    val visible: StateFlow<Boolean> = combine(guide.config, store.seen) { _, seen -> evaluate(seen) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, evaluate(store.seen.value))

    init {
        // 화면에 들어올 때마다 갱신한다. DEBUG 는 fetch interval 이 0 이라 Console 게시가 바로 반영된다.
        viewModelScope.launch { guide.refresh() }
    }

    fun confirm() = store.setSeen(true)

    private fun evaluate(seen: Boolean): Boolean {
        val active = guide.isActive(NewFeatureKeys.V2_UPDATE_MODAL)
        val visible = active && !seen
        // android.util.Log 는 Robolectric 없는 JVM 테스트에서 링크 에러다. 진단 한 줄 때문에
        // Phase A 테스트를 Robolectric 으로 끌고 갈 이유는 없어 System.out 으로 남긴다(logcat 에 그대로 찍힌다).
        if (BuildConfig.DEBUG) {
            println("NewFeatureGuide: v2_update_modal isActive=$active hasSeen=$seen visible=$visible")
        }
        return visible
    }
}
