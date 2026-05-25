package com.pickflow.android.core.network

class ApiException(
    val code: String,
    message: String,
) : RuntimeException(message)
