package com.pickflow.android.core.services.impl

import com.pickflow.android.core.network.api.SpotReportApi
import com.pickflow.android.core.network.dto.report.SpotReportRequest
import com.pickflow.android.core.network.unwrap
import com.pickflow.android.core.services.protocols.SpotReportService
import javax.inject.Inject

class DefaultSpotReportService @Inject constructor(
    private val spotReportApi: SpotReportApi,
) : SpotReportService {
    override suspend fun report(spotId: Long, content: String): Long =
        spotReportApi.report(spotId, SpotReportRequest(content = content))
            .unwrap()
            .reportId
}
