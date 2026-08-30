package com.pickflow.android.feature.map

import com.pickflow.android.core.services.protocols.Coordinates

/**
 * 탐색 지역 필터 — 로고 우측 지역명 + 지역 선택 바텀시트가 쓴다.
 *
 * 선언 순서 = 바텀시트 표시 순서(별도 정렬 없음).
 * 서버에 지역 파라미터가 없으므로 "지역 적용"은 지도 카메라를 [center] 로 옮기는 것으로 구현한다.
 * 카메라가 멈추면 기존 viewport 재조회가 그 지역 스팟을 가져온다.
 */
enum class Region(val displayName: String, val center: Coordinates) {
    Daejeon("대전", Coordinates(36.3504, 127.3845)),
    Seoul("서울", Coordinates(37.5665, 126.9780)),
}
