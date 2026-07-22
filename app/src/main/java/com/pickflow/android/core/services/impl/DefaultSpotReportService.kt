package com.pickflow.android.core.services.impl

import com.pickflow.android.core.network.ApiException
import com.pickflow.android.core.network.api.SpotReportApi
import com.pickflow.android.core.network.dto.report.SpotReportRequest
import com.pickflow.android.core.services.protocols.SpotReportService
import javax.inject.Inject

class DefaultSpotReportService @Inject constructor(
    private val spotReportApi: SpotReportApi,
) : SpotReportService {
    /**
     * iOS 는 신고 응답 본문을 디코딩하지 않는다(`EmptyResponse`).
     * 서버가 `data: null` 로 성공 응답을 주므로 data 부재를 실패로 취급하지 않는다.
     */
    override suspend fun report(spotId: Long, content: String): Long {
        val response = spotReportApi.report(spotId, SpotReportRequest(content = content))
        if (!response.success) throw ApiException(response.code, response.message)
        return response.data?.reportId ?: 0L
    }
}
