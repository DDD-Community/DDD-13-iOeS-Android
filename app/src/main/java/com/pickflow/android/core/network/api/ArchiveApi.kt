package com.pickflow.android.core.network.api

import com.pickflow.android.core.network.ApiResponse
import com.pickflow.android.core.network.dto.archive.ArchiveImageResponseDto
import retrofit2.http.GET

interface ArchiveApi {
    @GET("v1/users/me/archive")
    suspend fun getArchive(): ApiResponse<ArchiveImageResponseDto>
}
