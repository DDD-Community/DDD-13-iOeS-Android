package com.pickflow.android.common.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val serverDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
private val noticeDisplayFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")

/**
 * 서버 날짜 문자열("yyyy-MM-dd")을 공지사항 표시 문자열("yyyy.MM.dd")로 변환.
 * 파싱 실패 시 원문 반환 — iOS `DateFormatter.noticeDisplayDate(from:)` 와 동일 폴백.
 */
fun formatNoticeDate(raw: String): String =
    runCatching { LocalDate.parse(raw, serverDateFormatter).format(noticeDisplayFormatter) }
        .getOrDefault(raw)
