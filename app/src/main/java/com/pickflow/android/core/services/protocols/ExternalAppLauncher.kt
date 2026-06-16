package com.pickflow.android.core.services.protocols

interface ExternalAppLauncher {
    suspend fun openMap(latitude: Double, longitude: Double, label: String)
    suspend fun openUrl(url: String)
    suspend fun dial(phoneNumber: String)

    /**
     * Chrome Custom Tabs 로 in-app 풍 외부 페이지를 띄운다.
     * 약관/개인정보처리방침 등 노션 페이지 진입에 사용. 앱을 벗어나지 않고
     * 컬러 스킴/뒤로가기 기본 동작이 보장된다.
     */
    suspend fun openCustomTab(url: String)
}
