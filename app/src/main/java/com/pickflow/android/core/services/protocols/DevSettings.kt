package com.pickflow.android.core.services.protocols

import com.pickflow.android.BuildConfig
import kotlinx.coroutines.flow.StateFlow

/** Dev Mode 에서 고를 수 있는 API 서버. 기본값은 운영(prod). */
enum class ApiEnvironment(val label: String, val baseUrl: String) {
    DEV("개발 (dev)", BuildConfig.PICKFLOW_API_BASE_URL_DEV),
    PROD("운영 (prod)", BuildConfig.PICKFLOW_API_BASE_URL_PROD),
    ;

    /** 배지/버전 표기용 짧은 이름. */
    val shortLabel: String = name.lowercase()

    companion object {
        /**
         * 저장된 선택이 없을 때 쓰는 환경 — 빌드 타입이 원래 보던 서버 그대로.
         * (debug = dev, release = prod. build.gradle.kts 의 buildTypes 참고)
         */
        val DEFAULT: ApiEnvironment =
            entries.firstOrNull { it.baseUrl == BuildConfig.PICKFLOW_API_BASE_URL } ?: PROD
    }
}

/**
 * Dev Mode 설정 저장소 — 앱을 껐다 켜도 유지된다.
 *
 * 네트워크 인터셉터가 요청 스레드에서 동기로 읽으므로 값은 항상 즉시 조회 가능해야 한다.
 */
interface DevSettings {
    val apiEnvironment: StateFlow<ApiEnvironment>
    val badgeEnabled: StateFlow<Boolean>
    val touchIndicatorEnabled: StateFlow<Boolean>

    /**
     * 상세 화면의 내 스팟 상태를 강제로 덮어쓴다(디버그 전용, null 이면 서버 값 그대로).
     *
     * 어드민 검수 API(`POST /v1/admin/spots/{id}/reviews`)가 `USER_ADMIN` 전용이라
     * 일반 계정으로는 반려·공개 상태를 만들 수 없다. 상태별 UI 를 실기기에서 보기 위한 임시 장치다.
     * 어드민 도구나 시드 데이터가 생기면 지운다.
     */
    val forcedMySpotStatus: StateFlow<MySpotStatus?>

    fun setApiEnvironment(environment: ApiEnvironment)
    fun setBadgeEnabled(enabled: Boolean)
    fun setTouchIndicatorEnabled(enabled: Boolean)
    fun setForcedMySpotStatus(status: MySpotStatus?)
}
