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
        // TODO: BE `/v1/app/config/android` 오픈 전까지 임시 비활성화.
        // 매 앱 진입(ForceUpdateGate)마다 호출되어 서버에 에러 로그가 쌓이는 것을 막기 위함.
        // 엔드포인트 오픈 시 아래 한 줄로 복구:
        //   return api.getAndroidVersionPolicy().unwrap().toDomain()
        return AppVersionPolicy(
            minimumVersion = "0.0.0",
            latestVersion = "0.0.0",
            forceUpdate = false,
            storeUrl = "",
            supportEmail = null,
            termsPolicies = null,
        )
    }
}
