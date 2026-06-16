package com.pickflow.android.feature.map.clustering

import com.naver.maps.geometry.LatLng
import com.naver.maps.map.clustering.ClusteringKey

/**
 * Naver Maps Android SDK `Clusterer` 가 받는 클러스터링 키.
 *
 * iOS `MapSpotClusterKey`(NMCClusteringKey) 1:1 대응 — `spotId` 가 동일하면 동일
 * 데이터로 간주된다(`equals` / `hashCode`).
 */
class MapSpotClusterKey(
    val spotId: Long,
    private val position: LatLng,
) : ClusteringKey {

    override fun getPosition(): LatLng = position

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MapSpotClusterKey) return false
        return spotId == other.spotId
    }

    override fun hashCode(): Int = spotId.hashCode()
}
