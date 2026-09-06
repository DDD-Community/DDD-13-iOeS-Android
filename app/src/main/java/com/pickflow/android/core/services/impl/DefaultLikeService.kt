package com.pickflow.android.core.services.impl

import com.pickflow.android.core.network.api.LikeApi
import com.pickflow.android.core.network.unwrap
import com.pickflow.android.core.services.protocols.LikeService
import javax.inject.Inject
import javax.inject.Singleton

/** 좋아요 등록/해제 모두 서버가 단일 출처다. 로컬 캐시를 두지 않는다. */
@Singleton
class DefaultLikeService @Inject constructor(
    private val likeApi: LikeApi,
) : LikeService {
    override suspend fun add(spotId: String): Long =
        likeApi.like(spotId.toLongIdOrThrow()).unwrap().likeCount

    override suspend fun remove(spotId: String): Long =
        likeApi.unlike(spotId.toLongIdOrThrow()).unwrap().likeCount

    private fun String.toLongIdOrThrow(): Long = toLongOrNull()
        ?: throw IllegalArgumentException("spotId는 정수여야 합니다: $this")
}
