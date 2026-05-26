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

/** iOS `ArchiveEmptyView` 1:1 — 저장된 스팟이 없을 때 안내 + "스팟 둘러보기" CTA. */
@Composable
fun ArchiveEmptyContent(
    onExploreClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .testTag("archive-empty"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "마음에 드는 스팟을\n발견하셨나요?",
                style = PickflowTypography.headingMedium,
                color = PickflowColors.gray30,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "나만의 출사 리스트를 채워보세요.\n저장된 스팟은 여기서 언제든 확인할 수 있어요.",
                style = PickflowTypography.bodyLarge,
                color = PickflowColors.gray50,
                textAlign = TextAlign.Center,
            )
        }
        Box(
            modifier = Modifier
                .padding(top = 36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(PickflowColors.sunsetOrange)
                .clickable(onClick = onExploreClick)
                .padding(horizontal = 24.dp, vertical = 10.dp)
                .testTag("archive-empty-explore"),
        ) {
            Text(
                text = "스팟 둘러보기",
                style = PickflowTypography.bodyMediumBold,
                color = PickflowColors.gray0,
            )
        }
    }
}
