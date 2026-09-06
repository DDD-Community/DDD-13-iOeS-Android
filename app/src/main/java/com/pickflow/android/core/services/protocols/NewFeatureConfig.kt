package com.pickflow.android.core.services.protocols

import kotlinx.serialization.Serializable

/**
 * Remote Config 파라미터 `new_feature_flags`(JSON) 의 스키마.
 *
 * **iOS(PV-80)와 같은 Firebase 프로젝트의 같은 파라미터를 공유한다.** 키·필드명·판정 규칙은
 * 양쪽이 함께 지켜야 하는 계약이라 한쪽만 바꾸면 안 된다(PV-79 §1~4).
 */
@Serializable
data class NewFeatureConfig(
    val features: List<NewFeature> = emptyList(),
)

/**
 * 신규 기능 안내 하나. [key] 만 필수고 나머지는 전부 optional 이다.
 *
 * 시각은 전부 **밀리초 epoch**. [startAt] 이 있으면 전 유저 공통 스케줄("런칭 기준"),
 * 없으면 기기별 최초 판정 시각 기준("유저 기준")으로 갈린다 — [NewFeatureGuide.isActive].
 */
@Serializable
data class NewFeature(
    val key: String,
    val startAt: Long? = null,
    /** 노출 종료 시각. 경계 **미포함**(now < endAt). */
    val endAt: Long? = null,
    val durationDays: Int? = null,
)

/** iOS 와 공유하는 feature 키 — 문자열을 화면 쪽에 흩뿌리지 않기 위한 상수. */
object NewFeatureKeys {
    /** 앱 진입 전체화면 모달. 로그인 여부와 무관하게 전 유저 대상이다(PV-79 §6). */
    const val V2_UPDATE_MODAL = "v2_update_modal"

    /**
     * 보관함 > 나만의 스팟 탭 바텀시트.
     *
     * 안드로이드에는 대응 바텀시트가 아직 없어 **키만 예약**해 둔다. 화면이 생기면
     * [NewFeatureGuide.isActive] 에 이 키를 넣기만 하면 된다(계정별 "봤음" 분리는 그때 추가).
     */
    const val SPOT_OPEN_GUIDE = "spot_open_guide"

    /** 탐색 탭 무드 필터 칩(햇살·야경)의 신규 dot. */
    const val HOME_NEW_BADGE = "home_new_badge"
}

/**
 * Remote Config 에서 [NewFeatureConfig] 를 가져오는 통로.
 *
 * Firebase 를 직접 만지는 곳은 이 구현체 하나뿐이고 판정 로직은 갖지 않는다 —
 * 그래야 [NewFeatureGuide] 테스트가 Firebase 없이 fake 로 돈다.
 */
interface NewFeatureConfigProvider {
    /** 원격에서 새로 받아 활성화한다. 실패하면 던진다(폴백은 [NewFeatureGuide] 가 한다). */
    suspend fun fetchFeatureConfig(): NewFeatureConfig

    /** 네트워크 없이 이미 activate 된 값. 없으면 null. */
    fun activatedFeatureConfig(): NewFeatureConfig?
}
