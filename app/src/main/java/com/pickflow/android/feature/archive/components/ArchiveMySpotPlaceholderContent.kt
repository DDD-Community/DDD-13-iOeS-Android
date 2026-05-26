package com.pickflow.android.feature.archive.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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

/** iOS `ArchiveMySpotPlaceholder` 1:1 — "나만의 스팟" 탭의 비어있음 상태 + 등록 CTA. */
@Composable
fun ArchiveMySpotPlaceholderContent(
    onRegisterClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .testTag("archive-myspot-placeholder"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "나만 아는 특별한 스팟을\n기록해 보세요!",
                style = PickflowTypography.headingSmall,
                color = PickflowColors.gray0,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "직접 촬영한 사진과 함께 스팟을 등록하고,\n나만의 포토 지도를 완성해 보세요.",
                style = PickflowTypography.bodyMedium,
                color = PickflowColors.gray50,
                textAlign = TextAlign.Center,
            )
        }
        Box(
            modifier = Modifier
                .padding(top = 36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(PickflowColors.sunsetOrange)
                .clickable(onClick = onRegisterClick)
                .padding(horizontal = 47.dp, vertical = 14.dp)
                .testTag("archive-myspot-register"),
        ) {
            Text(
                text = "첫 번째 스팟 등록하기",
                style = PickflowTypography.bodyLargeBold,
                color = PickflowColors.gray0,
            )
        }
    }
}
