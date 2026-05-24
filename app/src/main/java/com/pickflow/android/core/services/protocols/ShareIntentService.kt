package com.pickflow.android.core.services.protocols

data class SharePayload(val title: String, val url: String)

interface ShareIntentService {
    suspend fun share(payload: SharePayload)
}
