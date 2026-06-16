package com.pickflow.android.common.util

/**
 * iOS `SpotIDCoder.swift` 1:1 — base-62 인코딩 (`type` + `-` + `id`).
 * `type=1` 이 spot 도메인. 알파벳/separator 모두 iOS 와 동일해야 양 플랫폼 share URL 호환.
 */
object SpotIdCoder {
    private val alphabet: CharArray =
        "5kRmHvNpLqT8sYuWdXjZcFbGiOeA9n2BgVrMo3CQfthE0SaKwIPDy61lJU74xz".toCharArray()
    private val base: Long = alphabet.size.toLong()
    private const val SEPARATOR = '-'
    private const val SPOT_TYPE: Long = 1

    /** spot id → "k-3mHv" 형태. */
    fun encodeSpot(id: Long): String =
        encode(SPOT_TYPE) + SEPARATOR + encode(id)

    /** "k-3mHv" → spot id. type 이 1 이 아니거나 잘못된 문자가 섞이면 null. */
    fun decodeSpot(token: String): Long? {
        val parts = token.split(SEPARATOR, limit = 2)
        if (parts.size != 2) return null
        val type = decode(parts[0]) ?: return null
        if (type != SPOT_TYPE) return null
        return decode(parts[1])
    }

    private fun encode(value: Long): String {
        if (value < 0) return ""
        var num = value
        val chars = StringBuilder()
        do {
            chars.insert(0, alphabet[(num % base).toInt()])
            num /= base
        } while (num > 0)
        return chars.toString()
    }

    private fun decode(encoded: String): Long? {
        var result = 0L
        for (ch in encoded) {
            val idx = alphabet.indexOf(ch)
            if (idx < 0) return null
            val offset = idx.toLong()
            if (result > (Long.MAX_VALUE - offset) / base) return null
            result = result * base + offset
        }
        return result
    }
}
