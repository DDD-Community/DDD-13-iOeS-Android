package com.pickflow.android.core.network

import android.content.ContentResolver
import android.net.Uri
import com.pickflow.android.core.services.protocols.ImagePayload
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * [ImagePayload] → multipart Part 변환의 단일 소스.
 * 스팟 등록(사진)·보관함(커버)·마이페이지(프로필) 업로드가 모두 이 함수에 의존한다.
 */
fun ImagePayload.toMultipartPart(partName: String): MultipartBody.Part {
    val body: RequestBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
    return MultipartBody.Part.createFormData(partName, filename, body)
}

fun Uri.toMultipartPart(
    resolver: ContentResolver,
    partName: String,
    fallbackFilename: String = "upload.bin",
    fallbackMime: String = "application/octet-stream",
): MultipartBody.Part {
    val mime = resolver.getType(this) ?: fallbackMime
    val bytes = resolver.openInputStream(this)
        ?.use { it.readBytes() }
        ?: throw IllegalStateException("Uri를 읽을 수 없습니다: $this")
    val body: RequestBody = bytes.toRequestBody(mime.toMediaTypeOrNull())
    return MultipartBody.Part.createFormData(partName, fallbackFilename, body)
}
