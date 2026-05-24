package com.pickflow.android.core.services.impl

import com.pickflow.android.core.services.protocols.SharePayload
import com.pickflow.android.core.services.protocols.ShareIntentService
import javax.inject.Inject

class NoopShareIntentService @Inject constructor() : ShareIntentService {
    override suspend fun share(payload: SharePayload) = Unit
}
