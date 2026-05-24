package com.pickflow.android.core.services.protocols

data class Cluster(
    val latitude: Double,
    val longitude: Double,
    val count: Int,
    val spotIds: List<String>,
)

interface ClusteringService {
    suspend fun cluster(spots: List<Spot>, zoomLevel: Int): List<Cluster>
}
