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
        // TODO: `/v1/app/config/android` 오픈 전까지 임시로 iOS config 를 사용한다(약관/정책 수신).
        //   iOS 정책의 forceUpdate 를 android 에 그대로 적용하면 오판하므로 강제 업데이트는 비활성화.
        return api.getIosVersionPolicy().unwrap().toDomain().copy(forceUpdate = false)
    }
}
