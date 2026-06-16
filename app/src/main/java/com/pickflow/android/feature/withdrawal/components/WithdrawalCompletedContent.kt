package com.pickflow.android.feature.withdrawal.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pickflow.android.R
import com.pickflow.android.common.designsystem.PickflowColors
import com.pickflow.android.common.designsystem.PickflowTypography

/**
 * iOS 탈퇴 완료 화면 1:1 — help 아이콘 + 안내 문구 + "로그인 화면으로 이동" 버튼.
 * 완료 상태에서는 back 도 로그인 이동으로 처리(호출 측 BackHandler).
 */
@Composable
fun WithdrawalCompletedContent(
    onGoToLogin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PickflowColors.gray95)
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp)
            .testTag("withdrawal-completed"),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))

        Image(
            painter = painterResource(R.drawable.ic_help_outline),
            contentDescription = null,
            modifier = Modifier.size(56.dp),
        )
        Spacer(Modifier.height(20.dp))
        Text(
            text = "탈퇴가 완료되었어요",
            style = PickflowTypography.headingMedium,
            color = PickflowColors.gray0,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "그동안 픽플로우를 이용해 주셔서 감사합니다.\n언제든 다시 찾아와 주세요.",
            style = PickflowTypography.bodyMedium,
            color = PickflowColors.gray40,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.weight(1f))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(PickflowColors.sunsetOrange)
                .clickable(onClick = onGoToLogin)
                .testTag("withdrawal-completed-login"),
            contentAlignment = Alignment.Center,
        ) {
            Text("로그인 화면으로 이동", style = PickflowTypography.bodyLargeBold, color = PickflowColors.gray0)
        }
    }
}
