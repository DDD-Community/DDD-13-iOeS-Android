package com.pickflow.android.core.network.api

import com.pickflow.android.core.network.ApiResponse
import com.pickflow.android.core.network.dto.report.SpotReportRequest
import com.pickflow.android.core.network.dto.report.SpotReportResponseDto
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

interface SpotReportApi {
    @POST("v1/spots/{spotId}/reports")
    suspend fun report(
        @Path("spotId") spotId: Long,
        @Body request: SpotReportRequest,
    ): ApiResponse<SpotReportResponseDto>
}
