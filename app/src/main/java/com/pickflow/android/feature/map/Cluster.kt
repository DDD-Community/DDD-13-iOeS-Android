package com.pickflow.android.feature.map

/**
 * 사용자 액션(셀 탭 → 바텀시트 노출)에 사용되는 클러스터 표현.
 *
 * Naver Maps Android SDK 의 `Clusterer` 가 내부에서 직접 화면-픽셀 거리 기반으로
 * 클러스터를 계산하므로(이전엔 자체 GridClusteringService 사용) 이 모델은 더 이상
 * 클러스터링 입력이 아니다. NaverMapView 가 leaf/cluster 탭 콜백에서 만들어 ViewModel
 * 로 전달하는 "선택된 클러스터" 의미만 갖는다.
 */
data class Cluster(
    val latitude: Double,
    val longitude: Double,
    val count: Int,
    val spotIds: List<String>,
)
