package com.pickflow.android.feature.myprofile.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pickflow.android.common.designsystem.PickflowColors
import com.pickflow.android.common.designsystem.PickflowTypography

/**
 * iOS `loadingScreen()` 대응 — gray95 위 중앙 ProgressView.
 * Paparazzi는 indeterminate 애니메이션을 0프레임으로 캡처하므로
 * 결정성을 위해 정적 호(arc)로 렌더한다.
 */
@Composable
fun MyProfileLoadingContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PickflowColors.gray95)
            .testTag("myprofile-loading"),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(
            progress = { 0.75f },
            color = PickflowColors.gray0,
            strokeWidth = 2.dp,
            modifier = Modifier.size(20.dp),
        )
    }
}

/**
 * iOS `failedScreen(message:)` 대응 — gray95 위 중앙 경고 아이콘 + 메시지.
 * iOS `exclamationmark.triangle`(36pt) 자리는 Material `Warning` 아이콘으로 치환.
 */
@Composable
fun MyProfileFailedContent(
    message: String = "네트워크 연결을 확인해주세요.",
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PickflowColors.gray95)
            .padding(horizontal = 24.dp)
            .testTag("myprofile-failed"),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Filled.Warning,
            contentDescription = null,
            tint = PickflowColors.sunsetOrange,
            modifier = Modifier.size(36.dp),
        )
        androidx.compose.foundation.layout.Spacer(Modifier.size(16.dp))
        Text(
            text = message,
            style = PickflowTypography.bodyMedium,
            color = PickflowColors.gray30,
            textAlign = TextAlign.Center,
        )
    }
}
