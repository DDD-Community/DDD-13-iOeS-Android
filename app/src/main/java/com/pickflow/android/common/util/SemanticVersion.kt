package com.pickflow.android.common.util

/**
 * iOS `SemanticVersion` 1:1 — `MAJOR.MINOR.PATCH` 비교.
 * 잘못된 문자열은 null 반환 (parser 실패).
 */
data class SemanticVersion(val major: Int, val minor: Int, val patch: Int) : Comparable<SemanticVersion> {
    override fun compareTo(other: SemanticVersion): Int = when {
        major != other.major -> major - other.major
        minor != other.minor -> minor - other.minor
        else -> patch - other.patch
    }

    companion object {
        fun parse(raw: String): SemanticVersion? {
            val parts = raw.split('.').mapNotNull { it.trim().toIntOrNull() }
            if (parts.size != 3) return null
            if (parts.any { it < 0 }) return null
            return SemanticVersion(parts[0], parts[1], parts[2])
        }
    }
}
