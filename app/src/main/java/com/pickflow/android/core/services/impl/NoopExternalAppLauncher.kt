package com.pickflow.android.core.services.impl

import com.pickflow.android.core.services.protocols.ExternalAppLauncher
import javax.inject.Inject

class NoopExternalAppLauncher @Inject constructor() : ExternalAppLauncher {
    override suspend fun openMap(latitude: Double, longitude: Double, label: String) = Unit
    override suspend fun openUrl(url: String) = Unit
    override suspend fun openCustomTab(url: String) = Unit
    override suspend fun dial(phoneNumber: String) = Unit
}
