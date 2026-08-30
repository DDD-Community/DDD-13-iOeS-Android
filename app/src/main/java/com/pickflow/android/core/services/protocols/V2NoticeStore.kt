package com.pickflow.android.core.services.protocols

/**
 * V2 업데이트 안내 팝업을 이미 봤는지 — 기기당 최초 1회만 띄우기 위한 로컬 플래그.
 */
interface V2NoticeStore {
    suspend fun isSeen(): Boolean
    suspend fun markSeen()
}
