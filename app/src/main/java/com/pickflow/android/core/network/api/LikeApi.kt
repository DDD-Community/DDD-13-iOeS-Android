package com.pickflow.android.core.network.api

import com.pickflow.android.core.network.ApiResponse
import com.pickflow.android.core.network.dto.like.LikeResponseDto
import retrofit2.http.DELETE
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * 좋아요(UI 용어로는 "추천"). `BookmarkApi` 와 같은 형태의 전용 인터페이스다.
 * 성공 응답의 `likeCount` 는 서버 최종값이지만, 화면은 낙관값을 유지한다
 * (`docs/PV-41/09-api-mapping.md` A 표).
 */
interface LikeApi {
    /** 등록 성공은 201. 이미 좋아요면 409 SL001 — non-2xx 라 HttpException 으로 올라온다. */
    @POST("v1/spots/{spotId}/likes")
    suspend fun like(@Path("spotId") spotId: Long): ApiResponse<LikeResponseDto>

    /** 좋아요 안 한 스팟 취소는 400 SL002 — non-2xx 라 HttpException 으로 올라온다. */
    @DELETE("v1/spots/{spotId}/likes")
    suspend fun unlike(@Path("spotId") spotId: Long): ApiResponse<LikeResponseDto>
}
