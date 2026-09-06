package com.pickflow.android.feature.spotregistration.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pickflow.android.common.designsystem.PickflowColors
import com.pickflow.android.common.designsystem.PickflowTypography

/**
 * 스팟 보완 폼 이탈 확인 팝업. 스크림·카드 규격은 [LogoutConfirmDialogOverlay] 와 같고
 * 버튼만 (계속하기 / 나가기)다.
 */
@Composable
fun RegistrationExitDialogOverlay(
    onContinue: () -> Unit = {},
    onExit: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(onClick = onContinue)
            .padding(horizontal = 32.dp)
            .testTag("registration-exit-dialog"),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(PickflowColors.gray90)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "이대로 나갈까요?",
                    style = PickflowTypography.headingSmall,
                    color = PickflowColors.gray0,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "등록하지 않은 내용은 사라져요.",
                    style = PickflowTypography.bodyMedium,
                    color = PickflowColors.gray40,
                    textAlign = TextAlign.Center,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(PickflowColors.gray0)
                        .clickable(onClick = onContinue)
                        .testTag("registration-exit-continue"),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "계속하기",
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
                        .clickable(onClick = onExit)
                        .testTag("registration-exit-confirm"),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "나가기",
                        style = PickflowTypography.bodyLargeBold,
                        color = PickflowColors.gray0,
                    )
                }
            }
        }
    }
}
