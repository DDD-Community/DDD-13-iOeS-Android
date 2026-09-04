package com.pickflow.android.core.services.protocols

/**
 * 탐색 지역 필터 — 로고 우측 지역명 + 지역 선택 바텀시트가 쓴다.
 *
 * [id] 는 서버 `GET /v1/regions` 의 `regionId` 다(1=서울, 2=대전). 스팟 조회 API
 * (`/v1/spots`, `/v1/spots/viewport`)가 `regionId` 를 **필수**로 요구하므로 이 값이
 * 요청에 그대로 실린다. 선언 순서 = 서버 응답 순서 = 바텀시트 표시 순서.
 *
 * 서버가 지역 목록을 주긴 하지만 [center] 는 주지 않는다. 지역 적용 시 지도 카메라를
 * 그 지역으로 옮기는 데 좌표가 필요하므로 목록 자체는 enum 으로 고정한다. 지역이 늘면
 * 여기 한 줄을 추가한다.
 */
enum class Region(val id: Long, val displayName: String, val center: Coordinates) {
    Seoul(1, "서울", Coordinates(37.5665, 126.9780)),
    Daejeon(2, "대전", Coordinates(36.3504, 127.3845)),
}
