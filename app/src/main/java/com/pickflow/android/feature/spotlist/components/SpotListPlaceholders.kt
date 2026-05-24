package com.pickflow.android.feature.spotlist.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
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

/** iOS `SpotListEmptyView` 1:1 — 빈 결과 안내. */
@Composable
fun SpotListEmptyContent(modifier: Modifier = Modifier) {
    PlaceholderScaffold(modifier = modifier.testTag("spotlist-empty")) {
        Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = null,
            tint = PickflowColors.gray40,
            modifier = Modifier.size(48.dp),
        )
        Text(
            text = "조건에 맞는 스팟이 없어요",
            style = PickflowTypography.bodyLargeBold,
            color = PickflowColors.gray0,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "필터를 조정해 보세요.",
            style = PickflowTypography.bodySmall,
            color = PickflowColors.gray50,
            textAlign = TextAlign.Center,
        )
    }
}

/** iOS `SpotListFailedView` 1:1 — 실패 안내 + 다시 시도. */
@Composable
fun SpotListFailedContent(
    message: String = "네트워크 오류",
    modifier: Modifier = Modifier,
) {
    PlaceholderScaffold(modifier = modifier.testTag("spotlist-failed")) {
        // iOS `icErrorOutline` 커스텀 에셋 자리 — Material `Warning`으로 치환.
        Icon(
            imageVector = Icons.Filled.Warning,
            contentDescription = null,
            tint = PickflowColors.gray40,
            modifier = Modifier.size(48.dp),
        )
        Text(
            text = "스팟을 불러오지 못했어요",
            style = PickflowTypography.bodyLargeBold,
            color = PickflowColors.gray0,
        )
        Text(
            text = message,
            style = PickflowTypography.bodySmall,
            color = PickflowColors.gray50,
            textAlign = TextAlign.Center,
        )
        PillButton(label = "다시 시도")
    }
}

/** iOS `SpotListUnauthorizedLocationView` 1:1 — 위치 권한 안내. */
@Composable
fun SpotListUnauthorizedContent(modifier: Modifier = Modifier) {
    PlaceholderScaffold(modifier = modifier.testTag("spotlist-unauthorized")) {
        Icon(
            imageVector = Icons.Filled.LocationOn,
            contentDescription = null,
            tint = PickflowColors.gray40,
            modifier = Modifier.size(48.dp),
        )
        Text(
            text = "위치 권한이 필요해요",
            style = PickflowTypography.bodyLargeBold,
            color = PickflowColors.gray0,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "가까운 스팟을 보여드리려면\n위치 권한 허용이 필요합니다.",
            style = PickflowTypography.bodySmall,
            color = PickflowColors.gray50,
            textAlign = TextAlign.Center,
        )
        PillButton(label = "설정으로 이동")
    }
}

@Composable
private fun PlaceholderScaffold(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PickflowColors.gray95)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        content()
    }
}

@Composable
private fun PillButton(label: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(PickflowColors.sunsetOrange)
            .padding(horizontal = 24.dp, vertical = 10.dp),
    ) {
        Text(
            text = label,
            style = PickflowTypography.bodyMediumBold,
            color = PickflowColors.gray0,
        )
    }
}
