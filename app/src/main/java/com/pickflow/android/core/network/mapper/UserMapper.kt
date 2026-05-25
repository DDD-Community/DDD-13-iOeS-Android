package com.pickflow.android.core.network.mapper

import com.pickflow.android.core.network.dto.user.MypageHomeResponseDto
import com.pickflow.android.core.services.protocols.MyPageHome

fun MypageHomeResponseDto.toMyPageHome(): MyPageHome = MyPageHome(
    nickname = nickname,
    profileImageUrl = profileImageUrl?.takeIf { it.isNotBlank() },
    savedSpotCount = savedSpotCount,
    recordedSpotCount = recordedSpotCount,
)
