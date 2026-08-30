package com.pickflow.android.core.services.protocols

import kotlinx.coroutines.flow.StateFlow

/**
 * V2 업데이트 안내 팝업을 이미 봤는지 — 기기당 최초 1회만 띄우기 위한 로컬 플래그.
 *
 * Dev Mode 스위치가 이 값을 직접 뒤집어 팝업을 다시 띄우므로 [StateFlow] 로 노출한다.
 * (한 번 심고 마는 값이면 suspend 로 충분했다.)
 */
interface V2NoticeStore {
    val seen: StateFlow<Boolean>
    fun setSeen(seen: Boolean)
}
