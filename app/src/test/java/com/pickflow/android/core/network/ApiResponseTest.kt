package com.pickflow.android.core.network

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class ApiResponseTest {

    @Test
    fun `unwrap returns data on success`() {
        val response = ApiResponse(success = true, code = "OK", message = "", data = "hello")
        assertEquals("hello", response.unwrap())
    }

    @Test
    fun `unwrap throws ApiException on failure with preserved code`() {
        val response = ApiResponse<String>(success = false, code = "AUTH_001", message = "토큰 만료", data = null)
        val ex = assertThrows(ApiException::class.java) { response.unwrap() }
        assertEquals("AUTH_001", ex.code)
        assertEquals("토큰 만료", ex.message)
    }

    @Test
    fun `unwrap throws when success true but data null`() {
        val response = ApiResponse<String>(success = true, code = "OK", message = "", data = null)
        assertThrows(ApiException::class.java) { response.unwrap() }
    }
}
