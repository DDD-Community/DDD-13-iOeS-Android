package com.pickflow.android.app

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.pickflow.android.app.navigation.DeepLinkState
import com.pickflow.android.app.navigation.PickflowNavHost
import com.pickflow.android.common.designsystem.PickflowTheme
import com.pickflow.android.common.util.SpotIdCoder
import com.pickflow.android.feature.devmode.TouchIndicator
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Android 15(targetSdk 35)는 어차피 edge-to-edge 를 강제한다. 여기서 명시적으로 켜서
        // Android 14 이하도 같은 경로를 타게 만든다 — 기기별로 레이아웃이 갈리지 않고
        // 구형 기기로도 inset 회귀를 검증할 수 있다.
        // 이 앱은 다크 고정 팔레트(gray95 #131416)라 시스템 바 아이콘도 밝은 쪽으로 고정한다.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        handleDeepLink(intent)
        setContent {
            PickflowTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    TouchIndicator {
                        PickflowNavHost()
                    }
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
