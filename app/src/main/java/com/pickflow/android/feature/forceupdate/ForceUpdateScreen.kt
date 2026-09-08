package com.pickflow.android.feature.forceupdate

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pickflow.android.common.designsystem.PickflowColors
import com.pickflow.android.common.designsystem.PickflowTypography

/**
 * iOS `ForceUpdateView` 1:1 — 닫기 없는 풀스크린 안내.
 * "업데이트하기" → 스토어 URL `ACTION_VIEW` Intent.
 */
@Composable
fun ForceUpdateScreen(storeUrl: String) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PickflowColors.gray95)
            .testTag("force-update-screen"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                // 배경은 바깥 Box 가 칠하므로 여기선 콘텐츠만 올린다.
                // (edge-to-edge 강제 기기에서 "업데이트하기" 버튼이 내비바에 가리면 앱이 잠긴다)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                imageVector = Icons.Filled.KeyboardArrowUp,
                contentDescription = null,
                tint = PickflowColors.sunsetOrange,
                modifier = Modifier.size(72.dp),
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "업데이트가 필요해요",
                style = PickflowTypography.headingMedium,
                color = PickflowColors.gray0,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "안정적인 서비스 이용을 위해\n최신 버전으로 업데이트해 주세요.",
                style = PickflowTypography.bodyMedium,
                color = PickflowColors.gray40,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.weight(1f))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(PickflowColors.sunsetOrange)
                    .clickable {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(storeUrl))
                            .apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                        context.startActivity(intent)
                    }
                    .testTag("force-update-cta"),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "업데이트하기",
                    style = PickflowTypography.bodyLargeBold,
                    color = PickflowColors.gray0,
                )
            }
        }
    }
}
