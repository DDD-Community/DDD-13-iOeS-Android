package com.pickflow.android.core.services.protocols

/**
 * 스팟 신고 API. 신고 본문은 5~200자.
 */
interface SpotReportService {
    /**
     * @return reportId 신고 접수 식별자
     */
    suspend fun report(spotId: Long, content: String): Long
}
