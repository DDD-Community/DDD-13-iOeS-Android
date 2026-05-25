package com.pickflow.android.core.services.protocols

interface SpotListService {
    /**
     * GET /v1/spots 1:1 매핑.
     *
     * @param theme nullable — 미전달 시 전체.
     * @param page 0-base. 첫 페이지는 0.
     * @param coordinates sort=DISTANCE 시 필수, RECOMMENDED 시 무시 가능.
     * @param sort 정렬. 서버 enum은 DISTANCE / RECOMMENDED 2종.
     */
    suspend fun fetch(
        theme: SpotTheme?,
        page: Int,
        coordinates: Coordinates? = null,
        sort: SpotSort = SpotSort.RECOMMENDED,
    ): SpotPage
}

enum class SpotSort { DISTANCE, RECOMMENDED }
