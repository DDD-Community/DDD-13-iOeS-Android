package com.pickflow.android.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.pickflow.android.app.navigation.DeepLinkState
import com.pickflow.android.app.navigation.PickflowNavHost
import com.pickflow.android.common.designsystem.PickflowTheme
import com.pickflow.android.common.util.SpotIdCoder
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleDeepLink(intent)
        setContent {
            PickflowTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    PickflowNavHost()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }

    /**
     * iOS `PickflowApp.handleUniversalLink` 1:1 — host=`pickflow-api.us`, path 첫 segment 를
     * [SpotIdCoder.decodeSpot] 으로 복원해 [DeepLinkState] 에 적재.
     */
    private fun handleDeepLink(intent: Intent?) {
        val data: Uri = intent?.data ?: return
        if (data.host != "pickflow-api.us") return
        val token = data.pathSegments.firstOrNull() ?: return
        val spotId = SpotIdCoder.decodeSpot(token) ?: return
        DeepLinkState.setPendingSpotId(spotId)
    }
}
