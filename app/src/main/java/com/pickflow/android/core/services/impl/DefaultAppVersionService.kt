package com.pickflow.android.core.services.impl

import com.pickflow.android.core.network.api.AppVersionApi
import com.pickflow.android.core.network.dto.appversion.toDomain
import com.pickflow.android.core.network.unwrap
import com.pickflow.android.core.services.protocols.AppVersionPolicy
import com.pickflow.android.core.services.protocols.AppVersionService
import javax.inject.Inject

class DefaultAppVersionService @Inject constructor(
    private val api: AppVersionApi,
) : AppVersionService {
    override suspend fun fetchAndroidVersionPolicy(): AppVersionPolicy {
        return api.getAndroidVersionPolicy().unwrap().toDomain()
    }
}
