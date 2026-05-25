package com.pickflow.android.core.network.mapper

import com.pickflow.android.core.network.dto.auth.TokenResponseDto
import com.pickflow.android.core.network.dto.auth.UserProfileDto
import com.pickflow.android.core.services.protocols.SocialProvider
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class AuthMapperTest {

    @Test
    fun `toAuthenticatedSession maps tokens and profile with KAKAO provider`() {
        val dto = TokenResponseDto(
            accessToken = "a",
            refreshToken = "r",
            profile = UserProfileDto(
                userId = "u1",
                email = "kdy@example.com",
                nickname = "pickflower",
                profileImageUrl = "https://img/u1.png",
                provider = "KAKAO",
            ),
        )
        val session = dto.toAuthenticatedSession()
        assertEquals("a", session.tokens.accessToken)
        assertEquals("r", session.tokens.refreshToken)
        assertEquals("u1", session.profile.userId)
        assertEquals(SocialProvider.KAKAO, session.profile.provider)
    }

    @Test
    fun `toUserProfile coerces blank email and image to null and unknown provider falls back to KAKAO`() {
        val dto = UserProfileDto(
            userId = "u2",
            email = "",
            nickname = "no-mail",
            profileImageUrl = " ",
            provider = "???",
        )
        val profile = dto.toUserProfile()
        assertNull(profile.email)
        assertNull(profile.profileImageUrl)
        assertEquals(SocialProvider.KAKAO, profile.provider)
    }
}
