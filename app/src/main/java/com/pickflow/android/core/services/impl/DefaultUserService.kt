package com.pickflow.android.core.services.impl

import com.pickflow.android.core.network.api.UserApi
import com.pickflow.android.core.network.mapper.toMyPageHome
import com.pickflow.android.core.network.unwrap
import com.pickflow.android.core.services.protocols.MyPageHome
import com.pickflow.android.core.services.protocols.UserService
import javax.inject.Inject

class DefaultUserService @Inject constructor(
    private val userApi: UserApi,
) : UserService {
    override suspend fun fetchMyPage(): MyPageHome =
        userApi.getMyPageHome().unwrap().toMyPageHome()
}
