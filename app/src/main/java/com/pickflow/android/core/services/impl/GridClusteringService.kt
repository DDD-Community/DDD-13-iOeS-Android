package com.pickflow.android.core.services.impl

import com.pickflow.android.core.services.protocols.Cluster
import com.pickflow.android.core.services.protocols.ClusteringService
import com.pickflow.android.core.services.protocols.Spot
import javax.inject.Inject
import kotlin.math.pow

class GridClusteringService @Inject constructor() : ClusteringService {
    override suspend fun cluster(spots: List<Spot>, zoomLevel: Int): List<Cluster> {
        if (spots.isEmpty()) return emptyList()
        val gridSize = 0.5 / 2.0.pow(zoomLevel.coerceAtLeast(0).toDouble())
        return spots.groupBy {
            Pair(
                (it.latitude / gridSize).toInt(),
                (it.longitude / gridSize).toInt(),
            )
        }.map { (_, group) ->
            Cluster(
                latitude = group.map { it.latitude }.average(),
                longitude = group.map { it.longitude }.average(),
                count = group.size,
                spotIds = group.map { it.id },
            )
        }
    }
}
