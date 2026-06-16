package com.pickflow.android.feature.map.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pickflow.android.common.designsystem.PickflowColors
import com.pickflow.android.common.designsystem.PickflowTypography

/**
 * iOS `LocationPermissionDeniedPopup.swift` 1:1 — gray90 카드.
 * 상단 sunsetOrange info 아이콘 → 타이틀/설명 → 취소(gray0) / 설정으로 이동(sunsetOrange) 2버튼.
 */
@Composable
fun LocationPermissionDeniedPopup(
    onCancel: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(PickflowColors.gray90)
            .padding(16.dp)
            .testTag("location-permission-denied-popup"),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = androidx.compose.ui.res.painterResource(com.pickflow.android.R.drawable.ic_check_circle),
            contentDescription = null,
            modifier = Modifier
                .padding(top = 8.dp)
                .size(48.dp),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "필수 권한을 허용해주세요.",
                style = PickflowTypography.headingSmall,
                color = PickflowColors.gray0,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "위치 권한을 허용해주세요. 정확한 위치\n사용을 권장합니다.",
                style = PickflowTypography.bodyMedium,
                color = PickflowColors.gray50,
                textAlign = TextAlign.Center,
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(PickflowColors.gray0)
                    .clickable(onClick = onCancel)
                    .testTag("location-permission-cancel"),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "취소",
                    style = PickflowTypography.bodyLargeBold,
                    color = PickflowColors.gray80,
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(PickflowColors.sunsetOrange)
                    .clickable(onClick = onOpenSettings)
                    .testTag("location-permission-settings"),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "설정으로 이동",
                    style = PickflowTypography.bodyLargeBold,
                    color = PickflowColors.gray0,
                )
            }
        }
    }
}
