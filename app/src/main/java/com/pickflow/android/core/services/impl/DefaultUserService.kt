package com.pickflow.android.core.services.impl

import com.pickflow.android.core.network.ApiException
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
import okhttp3.MultipartBody

class DefaultUserService @Inject constructor(
    private val userApi: UserApi,
) : UserService {
    override suspend fun fetchMyPage(): MyPageHome =
        userApi.getMyPageHome().unwrap().toMyPageHome()

    /**
     * iOS `UserService.updateProfile` 1:1 — multipart PATCH.
     * `nickname` 은 텍스트 part, `profileImage` 는 파일 part 로 변경분만 전송한다.
     * iOS 는 응답 본문을 디코딩하지 않으므로(EmptyResponse) `data` 부재를 실패로 취급하지 않는다.
     */
    override suspend fun updateProfile(
        nickname: String?,
        profileImage: ImagePayload?,
    ): UpdateProfileResult {
        val parts = buildList {
            nickname?.let { add(MultipartBody.Part.createFormData(NICKNAME_PART, it)) }
            profileImage?.let { add(it.toMultipartPart(PART_NAME)) }
        }
        require(parts.isNotEmpty()) { "변경된 항목이 없습니다." }

        val response = userApi.updateProfile(parts)
        if (!response.success) throw ApiException(response.code, response.message)
        return response.data?.toUpdateProfileResult()
            ?: UpdateProfileResult(displayName = nickname.orEmpty(), profileImageUrl = null)
    }

    override suspend fun saveWithdrawalReason(type: WithdrawalReasonType, content: String?) {
        userApi.saveWithdrawalReason(
            WithdrawalReasonRequest(reasonType = type.name, content = content)
        ).unwrapVoid()
    }

    private companion object {
        const val PART_NAME = "profileImage"
        const val NICKNAME_PART = "nickname"
    }
}
