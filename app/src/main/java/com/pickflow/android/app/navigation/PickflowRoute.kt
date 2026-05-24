package com.pickflow.android.app.navigation

/** iOS AppRootView 라우팅 진입점 1:1 매핑. */
object PickflowRoute {
    const val ONBOARDING = "onboarding"
    const val LOGIN = "login"
    const val HOME = "home"
    const val SPOT_SEARCH = "spot_search"
    const val SPOT_REGISTRATION = "spot_registration"
    const val ACCOUNT_MANAGEMENT = "account_management"
    const val DEBUG = "debug"

    const val SPOT_DETAIL = "spot_detail/{spotId}"
    fun spotDetail(spotId: String) = "spot_detail/$spotId"
    const val ARG_SPOT_ID = "spotId"
}

/** Home 내부 하단 탭 — iOS 3-tab(탐색/저장/마이) 대응. */
enum class HomeTab(val route: String, val label: String) {
    EXPLORE("tab_explore", "탐색"),
    SAVED("tab_saved", "저장"),
    MY("tab_my", "마이"),
}
