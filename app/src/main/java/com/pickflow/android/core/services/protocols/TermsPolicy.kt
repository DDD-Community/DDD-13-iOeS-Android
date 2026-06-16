package com.pickflow.android.core.services.protocols

/**
 * iOS `TermsPolicy.swift` 1:1.
 * `GET /v1/app/config/android` 응답 `data.termsPolicies` 항목과 매핑.
 */
data class TermsPolicy(
    /** 문서 종류 식별자 (예: `TERMS_OF_SERVICE`). 제목과 무관하게 분기/정렬 기준. */
    val type: String,
    /** 화면에 노출되는 문서 제목. */
    val title: String,
    /** 문서 원문 웹 URL. */
    val url: String,
)
