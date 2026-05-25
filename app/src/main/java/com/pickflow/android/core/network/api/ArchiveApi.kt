package com.pickflow.android.core.network.api

import com.pickflow.android.core.network.ApiResponse
import com.pickflow.android.core.network.dto.archive.ArchiveImageResponseDto
import okhttp3.MultipartBody
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ArchiveApi {
    @GET("v1/users/me/archive")
    suspend fun getArchive(): ApiResponse<ArchiveImageResponseDto>

    @Multipart
    @POST("v1/users/me/archive")
    suspend fun updateArchiveImage(
        @Part archiveImage: MultipartBody.Part,
    ): ApiResponse<ArchiveImageResponseDto>
}
