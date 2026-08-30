package com.pickflow.android.core.services.protocols

/**
 * "비회원으로 시작하기"로 탐색 탭에 들어간 적이 있는지 기록한다.
 *
 * 이력이 있으면 다음 앱 실행부터 로그인 화면을 건너뛰고 바로 탐색 탭으로 보낸다.
 * (로그인 여부와는 별개 — [AuthService.isLoggedIn]이 우선한다.)
 */
interface GuestEntryStore {
    suspend fun hasEntered(): Boolean
    suspend fun setEntered(entered: Boolean)
}
