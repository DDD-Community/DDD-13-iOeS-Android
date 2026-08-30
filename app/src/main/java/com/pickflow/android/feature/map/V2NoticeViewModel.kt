package com.pickflow.android.feature.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pickflow.android.core.services.protocols.V2NoticeStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * V2 업데이트 안내 팝업 — 플래그가 비어 있는 기기에서 최초 1회만 노출.
 * "확인했어요" 를 누르면 플래그를 심어 다시 뜨지 않는다.
 */
@HiltViewModel
class V2NoticeViewModel @Inject constructor(
    private val store: V2NoticeStore,
) : ViewModel() {

    private val _visible = MutableStateFlow(false)
    val visible: StateFlow<Boolean> = _visible.asStateFlow()

    init {
        viewModelScope.launch { _visible.value = !store.isSeen() }
    }

    fun confirm() {
        _visible.value = false
        viewModelScope.launch { store.markSeen() }
    }
}
