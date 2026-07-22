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
     * iOS `PickflowApp.handleUniversalLink` 1:1 + 커스텀 스킴 폴백.
     *
     * - `https://pickflow-api.us/{token}` — App Links(검증 완료 시) 직행 경로.
     * - `pickflow://spot/{token}` — 공유 랜딩 페이지가 App Links 미검증 환경에서 여는 폴백.
     *
     * token 을 [SpotIdCoder.decodeSpot] 으로 복원해 [DeepLinkState] 에 적재.
     */
    private fun handleDeepLink(intent: Intent?) {
        val data: Uri = intent?.data ?: return
        val token = when {
            data.scheme == "pickflow" && data.host == "spot" -> data.pathSegments.firstOrNull()
            data.host == "pickflow-api.us" -> data.pathSegments.firstOrNull()
            else -> null
        } ?: return
        val spotId = SpotIdCoder.decodeSpot(token) ?: return
        DeepLinkState.setPendingSpotId(spotId)
    }
}
