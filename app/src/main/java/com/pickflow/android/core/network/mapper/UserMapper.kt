package com.pickflow.android.core.network.mapper

import com.pickflow.android.core.network.dto.user.MypageHomeResponseDto
import com.pickflow.android.core.network.dto.user.UpdateProfileResponseDto
import com.pickflow.android.core.services.protocols.MyPageHome
import com.pickflow.android.core.services.protocols.SocialProvider
import com.pickflow.android.core.services.protocols.UpdateProfileResult

fun MypageHomeResponseDto.toMyPageHome(): MyPageHome = MyPageHome(
    nickname = nickname,
    profileImageUrl = profileImageUrl?.takeIf { it.isNotBlank() },
    savedSpotCount = savedSpotCount,
    recordedSpotCount = recordedSpotCount,
    provider = SocialProvider.fromRaw(provider),
)

fun UpdateProfileResponseDto.toUpdateProfileResult(): UpdateProfileResult = UpdateProfileResult(
    displayName = displayName,
    profileImageUrl = profileImageUrl?.takeIf { it.isNotBlank() },
)
