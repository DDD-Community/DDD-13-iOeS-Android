package com.pickflow.android.core.network

import kotlinx.serialization.Serializable

@Serializable
data class ApiResponse<T>(
    val success: Boolean = false,
    val code: String = "",
    val message: String = "",
    val data: T? = null,
)

fun <T> ApiResponse<T>.unwrap(): T {
    if (!success) throw ApiException(code, message)
    return data ?: throw ApiException(code, "응답 데이터가 비어 있습니다.")
}

fun ApiResponse<Unit>.unwrapVoid() {
    if (!success) throw ApiException(code, message)
}
