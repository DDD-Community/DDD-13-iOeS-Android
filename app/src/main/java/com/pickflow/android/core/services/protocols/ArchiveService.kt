package com.pickflow.android.core.services.protocols

/**
 * 보관함(Archive) API. 사용자 1인당 보관함 1개로 가정.
 * - name: 보관함 이름 (최대 20자)
 * - imageUrl: presigned URL. 이미지 없으면 null.
 */
data class Archive(
    val name: String,
    val imageUrl: String?,
)

interface ArchiveService {
    suspend fun fetch(): Archive
}
