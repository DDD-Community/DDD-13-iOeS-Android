package com.pickflow.android.feature.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.pickflow.android.common.designsystem.PickflowColors
import com.pickflow.android.common.designsystem.PickflowTypography

/** iOS Debug 화면(#if DEBUG) 대응 — Analytics 샘플 트리거. */
@Composable
fun DebugScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PickflowColors.gray95)
            .padding(24.dp)
            .testTag("debug-screen"),
    ) {
        Text(
            text = "Debug",
            style = PickflowTypography.headingLarge,
            color = PickflowColors.gray0,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Analytics 이벤트 샘플 트리거 (DEBUG 전용).",
            style = PickflowTypography.bodyMedium,
            color = PickflowColors.gray30,
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onBack) { Text("닫기") }
    }
}
