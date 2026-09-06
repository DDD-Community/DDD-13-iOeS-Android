package com.pickflow.android.core.services.protocols

/**
 * 탐색 지역 필터 — 로고 우측 지역명 + 지역 선택 바텀시트가 쓴다.
 *
 * 목록은 서버 `GET /v1/regions` 가 준다([RegionCatalog]). [id] 는 그 응답의 `regionId` 이고,
 * 스팟 조회 API(`/v1/spots`, `/v1/spots/viewport`)가 `regionId` 를 **필수**로 요구하므로
 * 이 값이 요청에 그대로 실린다.
 *
 * 서버가 좌표는 주지 않아 [center] 만 앱이 [id] 로 붙인다. 좌표를 모르는 지역은 `null` 이고,
 * 이때 지역을 적용해도 카메라는 그대로 둔다(스팟은 `regionId` 로 걸러지므로 리스트는 정상).
 */
data class Region(val id: Long, val displayName: String) {

    val center: Coordinates? get() = CENTERS[id]

    companion object {
        /**
         * 지역별 지도 중심. 서울은 지도 최초 카메라(`NaverMapView.INITIAL_CAMERA`, 성수)와
         * 같은 좌표라 첫 화면과 "서울" 라벨이 어긋나지 않는다.
         *
         * ponytail: 서버 `RegionItem` 에 위경도가 실리면 이 표를 지우고 응답을 그대로 쓴다.
         */
        private val CENTERS = mapOf(
            1L to Coordinates(37.538, 127.038),
            2L to Coordinates(36.3504, 127.3845),
        )

        /** 서버·캐시 응답이 오기 전(그리고 둘 다 실패했을 때) 쓰는 부트스트랩 값. */
        val Seoul = Region(1, "서울")
        val Daejeon = Region(2, "대전")
        val FALLBACK = listOf(Seoul, Daejeon)
    }
}
