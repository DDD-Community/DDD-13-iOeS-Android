package com.pickflow.android.core.network

import retrofit2.HttpException

class ApiException(
    val code: String,
    message: String,
    /** 탈퇴 이력 계정 재가입(U007) 시 BE 가 내려주는 복구 토큰. 없으면 null. */
    val restoreToken: String? = null,
) : RuntimeException(message)

/** 탈퇴 이력 계정 재가입 필요(U007) 여부. */
fun Throwable.isWithdrawalRestoreRequired(): Boolean =
    this is ApiException && code.equals("U007", ignoreCase = true)

/**
 * 인증 실패(401) 여부.
 * - Retrofit 의 4xx 응답은 [HttpException] 으로 throw 되고,
 * - 200 OK + `success=false` 인 BE-정의 401은 [ApiException.code] 에 담긴다.
 * 두 경로 모두 커버한다.
 */
fun Throwable.isUnauthorized(): Boolean = when (this) {
    is HttpException -> code() == 401
    is ApiException -> code == "401" || code.equals("UNAUTHORIZED", ignoreCase = true)
    else -> false
}

