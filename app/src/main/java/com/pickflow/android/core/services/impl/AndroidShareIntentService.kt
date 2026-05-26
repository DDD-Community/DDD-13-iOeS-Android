package com.pickflow.android.core.services.impl

import android.content.Context
import android.content.Intent
import com.pickflow.android.core.services.protocols.SharePayload
import com.pickflow.android.core.services.protocols.ShareIntentService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** iOS `shareSheetPresenter.present(items:)` 1:1 — ACTION_SEND text/plain chooser. */
@Singleton
class AndroidShareIntentService @Inject constructor(
    @ApplicationContext private val context: Context,
) : ShareIntentService {

    override suspend fun share(payload: SharePayload) {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "${payload.title}\n${payload.url}")
        }
        val chooser = Intent.createChooser(sendIntent, payload.title).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
