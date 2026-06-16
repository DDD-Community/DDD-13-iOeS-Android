package com.pickflow.android.feature.login.components

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pickflow.android.R
import com.pickflow.android.common.designsystem.PickflowColors
import com.pickflow.android.common.designsystem.PickflowTypography

/**
 * iOS `RestoreAccountDialog` 1:1 — 탈퇴 이력 계정 재가입 안내 팝업.
 * 서버 message 가 있으면 제목으로 사용, 없으면 기본 문구.
 */
@Composable
fun RestoreAccountDialog(
    message: String?,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = message?.trim()?.takeIf { it.isNotEmpty() }
        ?: "기존 탈퇴 이력이 있습니다.\n재가입 하시겠습니까?"

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(onClick = onCancel)
            .padding(horizontal = 32.dp)
            .testTag("restore-account-dialog"),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(PickflowColors.gray90)
                .clickable(enabled = false) {}
                .padding(horizontal = 16.dp)
                .padding(top = 24.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_error_outline),
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = title,
                        style = PickflowTypography.headingSmall,
                        color = PickflowColors.gray0,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = "재가입 시 기존 회원 정보가 복구됩니다.",
                        style = PickflowTypography.bodyMedium,
                        color = PickflowColors.gray30,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                DialogButton(
                    text = "취소",
                    background = PickflowColors.gray0,
                    textColor = PickflowColors.gray80,
                    onClick = onCancel,
                    modifier = Modifier.weight(1f).testTag("restore-cancel"),
                )
                DialogButton(
                    text = "재가입",
                    background = PickflowColors.sunsetOrange,
                    textColor = PickflowColors.gray0,
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f).testTag("restore-confirm"),
                )
            }
        }
    }
}

@Composable
private fun DialogButton(
    text: String,
    background: Color,
    textColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = PickflowTypography.bodyLargeBold, color = textColor)
    }
}
