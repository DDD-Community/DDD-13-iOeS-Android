package com.pickflow.android.feature.spotdetail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pickflow.android.common.designsystem.PickflowColors
import com.pickflow.android.common.designsystem.PickflowTypography

/**
 * iOS `LoginPromptPopup.swift` 1:1 — gray90 카드 + 취소(gray0) / 간편 로그인하기(sunsetOrange) 2버튼.
 * 컨테이너 (반투명 오버레이 + tap-to-dismiss) 는 호출 측에서 제공.
 */
@Composable
fun LoginPromptPopup(
    onCancel: () -> Unit,
    onLogin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(PickflowColors.gray90)
            .padding(24.dp)
            .testTag("login-prompt-popup"),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "잠깐,\n로그인이 필요한 기능이에요",
            style = PickflowTypography.headingSmall,
            color = PickflowColors.gray0,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "마음에 드는 스팟을 놓치지 않도록\n지금 바로 연결해 보세요.\n간편 로그인으로 바로 시작할 수 있습니다.",
            style = PickflowTypography.bodyMedium,
            color = PickflowColors.gray50,
            textAlign = TextAlign.Center,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(PickflowColors.gray0)
                    .clickable(onClick = onCancel)
                    .testTag("login-prompt-cancel"),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "취소",
                    style = PickflowTypography.bodyLargeBold,
                    color = PickflowColors.gray95,
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(PickflowColors.sunsetOrange)
                    .clickable(onClick = onLogin)
                    .testTag("login-prompt-login"),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "간편 로그인하기",
                    style = PickflowTypography.bodyLargeBold,
                    color = PickflowColors.gray0,
                )
            }
        }
    }
}
