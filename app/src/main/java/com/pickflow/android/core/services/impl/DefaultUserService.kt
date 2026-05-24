package com.pickflow.android.core.services.impl

import com.pickflow.android.core.services.protocols.UserService
import javax.inject.Inject

class DefaultUserService @Inject constructor() : UserService {
    override suspend fun fetchUserName(): String = "Pickflow User"
}
