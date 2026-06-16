package com.pickflow.android.feature.spotdetail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
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
 * iOS `MySpotComingSoonSheet.swift` 1:1 — "나만의 포토 스팟, 곧 오픈할 수 있어요!" 안내.
 * 두 버튼: 괜찮아요(gray0 배경) / 업데이트 소식 받기(sunsetOrange + 알림 아이콘).
 *
 * BottomSheet 컨테이너 자체는 호출 측의 `ModalBottomSheet` 에서 제공한다.
 */
@Composable
fun MySpotComingSoonSheet(
    onCancel: () -> Unit,
    onNotify: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(PickflowColors.gray95)
            .padding(horizontal = 16.dp)
            .padding(bottom = 26.dp)
            .heightIn(min = 280.dp)
            .testTag("myspot-coming-soon-sheet"),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Spacer(modifier = Modifier.height(0.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "나만의 포토 스팟,\n곧 오픈할 수 있어요!",
                style = PickflowTypography.headingMedium,
                color = PickflowColors.gray0,
                textAlign = TextAlign.Center,
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "나만 알기 아까운 포토 스팟을\n세상에 공개할 준비가 되었나요?",
                    style = PickflowTypography.bodyMedium,
                    color = PickflowColors.gray30,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "기능이 업데이트되면 가장 먼저 알려드릴게요.",
                    style = PickflowTypography.bodyMediumBold,
                    color = PickflowColors.gray0,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .height(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(PickflowColors.gray0)
                    .clickable(onClick = onCancel)
                    .padding(horizontal = 30.dp)
                    .testTag("myspot-coming-cancel"),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "괜찮아요",
                    style = PickflowTypography.bodyLargeBold,
                    color = PickflowColors.gray80,
                )
            }

            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(PickflowColors.sunsetOrange)
                    .clickable(onClick = onNotify)
                    .testTag("myspot-coming-notify"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Notifications,
                    contentDescription = null,
                    tint = PickflowColors.gray0,
                    modifier = Modifier
                        .size(20.dp)
                        .padding(end = 6.dp),
                )
                Text(
                    text = "업데이트 소식 받기",
                    style = PickflowTypography.bodyLargeBold,
                    color = PickflowColors.gray0,
                )
            }
        }
    }
}
