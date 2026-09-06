package com.pickflow.android.app.navigation

/** iOS AppRootView 라우팅 진입점 1:1 매핑. */
object PickflowRoute {
    const val ONBOARDING = "onboarding"
    const val LOGIN = "login"
    const val HOME = "home"
    const val SPOT_SEARCH = "spot_search"
    const val SPOT_LOCATION_DETAIL = "spot_location_detail"
    const val SPOT_REGISTRATION = "spot_registration"
    const val SPOT_REGISTRATION_ROUTE =
        "spot_registration?reviseSpotId={reviseSpotId}"
    fun spotRevision(spotId: Long) = "spot_registration?reviseSpotId=$spotId"
    const val ARG_REVISE_SPOT_ID = "reviseSpotId"
    const val ACCOUNT_MANAGEMENT = "account_management"
    const val TERMS_AND_POLICY = "terms_and_policy"
    const val WITHDRAWAL = "withdrawal"
    const val DEV_MODE = "dev_mode"

    /**
     * 스팟 상세. 남의 스팟·큐레이션·내 스팟이 모두 이 하나를 쓴다.
     * 내 스팟이면 서버 `isMySpot`/`status` 에 따라 오픈 신청·철회·삭제가 화면 안에서 열린다.
     */
    const val SPOT_DETAIL = "spot_detail/{spotId}?registered={registered}"
    fun spotDetail(spotId: String, registered: Boolean = false) =
        "spot_detail/$spotId?registered=$registered"
    const val ARG_SPOT_ID = "spotId"
    const val ARG_REGISTERED = "registered"


    /** 공지사항 게시판(BoardService.posts/detail) — masterId 는 BuildConfig 주입. */
    const val NOTICE_LIST = "notice_list"
    const val NOTICE_DETAIL = "notice_detail/{postId}"
    fun noticeDetail(postId: Long) = "notice_detail/$postId"
    const val ARG_NOTICE_POST_ID = "postId"
}

/** Home 내부 하단 탭 — iOS 3-tab(탐색/저장/마이) 대응. */
enum class HomeTab(val route: String, val label: String) {
    EXPLORE("tab_explore", "탐색"),
    SAVED("tab_saved", "저장"),
    MY("tab_my", "마이"),
}
