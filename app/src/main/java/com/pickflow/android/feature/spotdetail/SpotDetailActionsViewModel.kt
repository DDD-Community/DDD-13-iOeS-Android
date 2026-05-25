package com.pickflow.android.feature.spotdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pickflow.android.core.services.protocols.ExternalAppLauncher
import com.pickflow.android.core.services.protocols.SpotDetail
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class SpotDetailActionsViewModel @Inject constructor(
    private val externalAppLauncher: ExternalAppLauncher,
) : ViewModel() {

    fun openInMap(spot: SpotDetail) {
        viewModelScope.launch {
            externalAppLauncher.openMap(spot.latitude, spot.longitude, spot.name)
        }
    }

    fun openWebsite(url: String) {
        viewModelScope.launch { externalAppLauncher.openUrl(url) }
    }
}
