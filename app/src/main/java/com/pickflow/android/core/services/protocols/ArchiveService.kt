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

    /**
     * 보관함 이미지를 업로드/교체한다. 기존 이미지가 있으면 서버에서 덮어쓰기.
     * 반환은 갱신된 보관함 상태 (이름 + 새 presigned URL).
     */
    suspend fun updateImage(image: ImagePayload): Archive
}
