package com.pickflow.android.core.services.impl

import com.pickflow.android.core.network.api.UserApi
import com.pickflow.android.core.network.dto.user.WithdrawalReasonRequest
import com.pickflow.android.core.network.mapper.toMyPageHome
import com.pickflow.android.core.network.mapper.toUpdateProfileResult
import com.pickflow.android.core.network.toMultipartPart
import com.pickflow.android.core.network.unwrap
import com.pickflow.android.core.network.unwrapVoid
import com.pickflow.android.core.services.protocols.ImagePayload
import com.pickflow.android.core.services.protocols.MyPageHome
import com.pickflow.android.core.services.protocols.UpdateProfileResult
import com.pickflow.android.core.services.protocols.UserService
import com.pickflow.android.core.services.protocols.WithdrawalReasonType
import javax.inject.Inject

class DefaultUserService @Inject constructor(
    private val userApi: UserApi,
) : UserService {
    override suspend fun fetchMyPage(): MyPageHome =
        userApi.getMyPageHome().unwrap().toMyPageHome()

    override suspend fun updateProfile(
        nickname: String?,
        profileImage: ImagePayload?,
    ): UpdateProfileResult {
        val dto = if (profileImage != null) {
            val part = profileImage.toMultipartPart(PART_NAME)
            userApi.updateProfileWithImage(nickname = nickname, profileImage = part).unwrap()
        } else {
            userApi.updateProfileNoImage(nickname = nickname).unwrap()
        }
        return dto.toUpdateProfileResult()
    }

    override suspend fun saveWithdrawalReason(type: WithdrawalReasonType, content: String?) {
        userApi.saveWithdrawalReason(
            WithdrawalReasonRequest(reasonType = type.name, content = content)
        ).unwrapVoid()
    }

    private companion object {
        const val PART_NAME = "profileImage"
    }
}
