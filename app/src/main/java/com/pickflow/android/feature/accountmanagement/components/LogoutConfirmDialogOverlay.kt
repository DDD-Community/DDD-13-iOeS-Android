package com.pickflow.android.feature.accountmanagement.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
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
 * iOS `LogoutConfirmDialog` 대응 — 로그아웃 확인 다이얼로그.
 *
 * 검정 0.6 스크림 위 흰 카드(좌우 32 패딩, corner 16). 안내 텍스트 +
 * 가로 구분선 + (취소 / 로그아웃) 2분할 버튼. 처리 중에는 로그아웃 버튼이
 * ProgressView로 바뀐다. Paparazzi 결정성을 위해 정적 호로 렌더한다.
 */
@Composable
fun LogoutConfirmDialogOverlay(
    isLoading: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .padding(horizontal = 32.dp)
            .testTag("logout-dialog"),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(PickflowColors.gray0),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 28.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "잠시 로그아웃하시겠어요?",
                    style = PickflowTypography.headingSmall,
                    color = PickflowColors.gray90,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "로그아웃해도 내 정보는 그대로 유지돼요.\n다시 로그인하면 언제든 이용할 수 있어요.",
                    style = PickflowTypography.bodyMedium,
                    color = PickflowColors.gray40,
                    textAlign = TextAlign.Center,
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(PickflowColors.gray10),
            )

            Row(modifier = Modifier.fillMaxWidth().height(56.dp)) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "취소",
                        style = PickflowTypography.bodyMediumBold,
                        color = PickflowColors.gray80,
                    )
                }
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(PickflowColors.gray10),
                )
                Box(
                    modifier = Modifier.weight(1f).fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            progress = { 0.75f },
                            color = PickflowColors.sunsetOrange,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp),
                        )
                    } else {
                        Text(
                            text = "로그아웃",
                            style = PickflowTypography.bodyMediumBold,
                            color = PickflowColors.sunsetOrange,
                        )
                    }
                }
            }
        }
    }
}
