package com.pickflow.android.core.services.protocols

/**
 * 공개 스팟의 노출(release) 여부 로컬 기억.
 *
 * `GET /v1/spots/{id}` 와 `GET /v1/users/me/my-spots` 응답에 노출 플래그가 없어
 * 화면에 다시 들어오면 토글이 항상 ON 으로 보이던 문제를 막는다.
 * 서버가 `released` 를 내려주면 이 저장소는 통째로 지운다
 * (`docs/PV-41/09-api-mapping.md` C 표).
 */
interface MySpotReleaseStore {
    /** 저장된 값이 없으면 true — 공개 직후 기본 상태가 노출이다. */
    fun released(spotId: Long): Boolean

    fun setReleased(spotId: Long, released: Boolean)
}
