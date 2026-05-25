package com.pickflow.android.core.network

import android.content.ContentResolver
import android.net.Uri
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody

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
