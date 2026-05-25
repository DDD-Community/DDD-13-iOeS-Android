package com.pickflow.android.core.network.api

import com.pickflow.android.core.network.ApiResponse
import com.pickflow.android.core.network.dto.board.BbsPostListResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

interface BoardApi {
    @GET("v1/bbs/posts")
    suspend fun getPosts(
        @Query("masterId") masterId: Long,
        @Query("page") page: Int? = null,
    ): ApiResponse<BbsPostListResponseDto>
}
