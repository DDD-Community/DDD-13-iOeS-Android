package com.pickflow.android.core.services.protocols

interface ExternalAppLauncher {
    suspend fun openMap(latitude: Double, longitude: Double, label: String)
    suspend fun openUrl(url: String)
    suspend fun dial(phoneNumber: String)
}
