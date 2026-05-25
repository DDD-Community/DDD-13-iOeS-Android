package com.pickflow.android.core.network.dto.report

import kotlinx.serialization.Serializable

@Serializable
data class SpotReportRequest(
    val content: String,
)

@Serializable
data class SpotReportResponseDto(
    val reportId: Long = 0L,
)
