package com.pickflow.android.core.network.api

import com.pickflow.android.core.network.ApiResponse
import com.pickflow.android.core.network.dto.myspot.CreateMySpotResponseDto
import com.pickflow.android.core.network.dto.myspot.CancelPublicationResponseDto
import com.pickflow.android.core.network.dto.myspot.MySpotListResponseDto
import com.pickflow.android.core.network.dto.myspot.OpenMySpotResponseDto
import com.pickflow.android.core.network.dto.myspot.ReleaseMySpotResponseDto
import com.pickflow.android.core.network.dto.myspot.UpdateMySpotResponseDto
import okhttp3.MultipartBody
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface MySpotApi {
    @GET("v1/users/me/my-spots")
    suspend fun getMySpots(
        @Query("page") page: Int? = null,
        @Query("latitude") latitude: Double? = null,
        @Query("longitude") longitude: Double? = null,
    ): ApiResponse<MySpotListResponseDto>

    /**
     * 나만의 스팟 등록. part 이름은 `image` + `request`(JSON 메타) —
     * iOS `SpotService.registerSpot` 과 동일(BE 검증 완료 형태).
     */
    @Multipart
    @POST("v1/users/me/my-spots")
    suspend fun createMySpot(
        @Part image: MultipartBody.Part,
        @Part meta: MultipartBody.Part,
    ): ApiResponse<CreateMySpotResponseDto>

    /**
     * 나만의 스팟 수정. 상태는 바뀌지 않는다.
     * `image` 미첨부 시 서버가 기존 이미지를 유지한다.
     */
    @Multipart
    @PUT("v1/users/me/my-spots/{spotId}")
    suspend fun updateMySpot(
        @Path("spotId") spotId: Long,
        @Part meta: MultipartBody.Part,
        @Part image: MultipartBody.Part? = null,
    ): ApiResponse<UpdateMySpotResponseDto>

    @DELETE("v1/users/me/my-spots/{spotId}")
    suspend fun deleteMySpot(@Path("spotId") spotId: Long): ApiResponse<Unit>

    /** 오픈 신청(검수 요청). `REJECTED → RE_REVIEW_PENDING` 재신청도 같은 경로다. */
    @POST("v1/users/me/my-spots/{spotId}/open-requests")
    suspend fun requestOpen(@Path("spotId") spotId: Long): ApiResponse<OpenMySpotResponseDto>

    /**
     * 노출 켜기. PUBLISHED 의 지도/리스트 노출 플래그만 바꾸고 status 는 그대로다.
     * 검수 flow 와 독립이라 재검수 없이 [unreleaseSpot] 과 왕복할 수 있다. PUBLISHED 가 아니면 SP012.
     */
    @POST("v1/users/me/my-spots/{spotId}/releases")
    suspend fun releaseSpot(
        @Path("spotId") spotId: Long,
    ): ApiResponse<ReleaseMySpotResponseDto>

    /** 노출 끄기. status 는 PUBLISHED 로 유지된다 — 공개 해제(DRAFT 전환)와 다르다. */
    @DELETE("v1/users/me/my-spots/{spotId}/releases")
    suspend fun unreleaseSpot(
        @Path("spotId") spotId: Long,
    ): ApiResponse<ReleaseMySpotResponseDto>

    /** 공개 해제 = 오픈 신청 철회 + 비공개 전환. 해제 후 상태는 항상 DRAFT. */
    @DELETE("v1/users/me/my-spots/{spotId}/publications")
    suspend fun cancelPublication(
        @Path("spotId") spotId: Long,
    ): ApiResponse<CancelPublicationResponseDto>
}
