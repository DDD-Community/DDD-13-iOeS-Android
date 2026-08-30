package com.pickflow.android.feature.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pickflow.android.core.services.protocols.V2NoticeStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * V2 업데이트 안내 팝업 — 플래그가 비어 있는 기기에서 최초 1회만 노출.
 * "확인했어요" 를 누르면 플래그를 심어 다시 뜨지 않는다(Dev Mode 스위치로 되돌릴 수 있다).
 */
@HiltViewModel
class V2NoticeViewModel @Inject constructor(
    private val store: V2NoticeStore,
) : ViewModel() {

    val visible: StateFlow<Boolean> = store.seen
        .map { !it }
        .stateIn(viewModelScope, SharingStarted.Eagerly, !store.seen.value)

    fun confirm() = store.setSeen(true)
}
